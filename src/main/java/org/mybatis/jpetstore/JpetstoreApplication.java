/*
 *    Copyright 2010-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.jpetstore;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application packaged
 * inside this archive (under {@code /webapp}), so no external application server is required.
 *
 * <p>
 * Configuration (12-factor, environment variables):
 * </p>
 * <ul>
 * <li>{@code PORT} - HTTP port (default 8080; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - context path (default "" i.e. root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_DIR = "/webapp";

  private JpetstoreApplication() {
  }

  /**
   * Start the embedded server.
   *
   * @param args
   *          command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getSetting("PORT", "8080"));
    String contextPath = normalizeContextPath(getSetting("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector().setProperty("address", getSetting("BIND_ADDRESS", "0.0.0.0"));

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    WebResourceRoot resources = new StandardRoot(context);
    resources.addPreResources(createWebappResources(resources));
    context.setResources(resources);

    tomcat.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors on shutdown
      }
    }));
    tomcat.getServer().await();
  }

  private static WebResourceSet createWebappResources(WebResourceRoot root) throws URISyntaxException {
    File location = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      return new JarResourceSet(root, "/", location.getAbsolutePath(), WEBAPP_RESOURCE_DIR);
    }
    File webappDir = new File(location, WEBAPP_RESOURCE_DIR);
    if (!webappDir.isDirectory()) {
      webappDir = new File("src/main/webapp");
    }
    return new DirResourceSet(root, "/", webappDir.getAbsolutePath(), "/");
  }

  private static String getSetting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.isEmpty() ? defaultValue : value;
  }

  private static String normalizeContextPath(String path) {
    if (path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }
}
