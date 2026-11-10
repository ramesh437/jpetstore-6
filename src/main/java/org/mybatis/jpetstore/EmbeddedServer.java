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
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the executable JAR. Starts an embedded Tomcat that serves the web application (previously packaged as
 * a WAR), so the application can run as a self-contained process on GKE, Cloud Run or App Engine flexible.
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port (default {@code 8080}, injected by Cloud Run)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default {@code /jpetstore}, use {@code /} for root)</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private EmbeddedServer() {
    // main class only
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", "/jpetstore"));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("docbase"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(EmbeddedServer.class.getClassLoader());
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    WebResourceRoot resources = new StandardRoot(context);
    File location = locateCodeSource();
    if (location.isDirectory()) {
      File webapp = new File(location, WEBAPP_RESOURCE_DIR);
      resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
    } else {
      resources.addPreResources(
          new JarResourceSet(resources, "/", location.getAbsolutePath(), "/" + WEBAPP_RESOURCE_DIR));
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors during shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File locateCodeSource() throws URISyntaxException {
    return new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String normalizeContextPath(String path) {
    if (path == null || path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
