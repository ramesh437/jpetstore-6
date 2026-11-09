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
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat and serves the web application packaged
 * under {@code /webapp} inside the archive (or {@code target/classes/webapp} when run from an IDE).
 *
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port (default 8080, injected by Cloud Run)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default {@code /jpetstore}; use empty or {@code /} for root)</li>
 * <li>{@code TOMCAT_BASE_DIR} - scratch directory for Tomcat/JSP work files (default: a temporary directory)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_INTERNAL_PATH = "/webapp";

  private JpetstoreApplication() {
    // entry point only
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Start the embedded server.
   *
   * @param args
   *          command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = env("CONTEXT_PATH", "/jpetstore");
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    Path baseDir = env("TOMCAT_BASE_DIR", null) != null ? Files.createDirectories(Path.of(env("TOMCAT_BASE_DIR", null)))
        : Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getServer().setPort(-1); // no shutdown port; stopped by SIGTERM

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());
    context.addApplicationListener("org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener");

    WebResourceRoot resources = new StandardRoot(context);
    File location = locateCodeSource();
    if (location.isDirectory()) {
      resources.addPostResources(new DirResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_INTERNAL_PATH));
    } else {
      resources
          .addPostResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_INTERNAL_PATH));
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors on shutdown
      }
    }, "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File locateCodeSource() throws URISyntaxException {
    return new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }
}
