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
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanner;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web
 * application bundled in the JAR (under {@code /webapp}), so no external application server is required.
 * <p>
 * Runtime configuration is read from environment variables (or JVM system properties of the same name):
 * </p>
 * <ul>
 * <li>{@code PORT} (or {@code SERVER_PORT}) - HTTP port, default {@code 8080} (Cloud Run injects {@code PORT})</li>
 * <li>{@code CONTEXT_PATH} - servlet context path, default empty (root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE = "webapp";

  private JpetstoreApplication() {
  }

  /**
   * Starts the embedded Tomcat server.
   *
   * @param args
   *          the command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getSetting("PORT", getSetting("SERVER_PORT", "8080")));
    String contextPath = normalizeContextPath(getSetting("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    baseDir.toFile().deleteOnExit();

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = tomcat.getConnector();
    connector.setPort(port);

    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));
    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    // Application classes are on the main class path; no need to scan the (fat) JAR for annotations/fragments.
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());
    StandardJarScanner scanner = new StandardJarScanner();
    scanner.setScanClassPath(true);
    context.setJarScanner(scanner);

    WebResourceRoot resources = new StandardRoot(context);
    addWebappResources(resources);
    context.setResources(resources);

    tomcat.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // ignore errors during shutdown
      }
    }));
    tomcat.getServer().await();
  }

  private static void addWebappResources(WebResourceRoot resources) throws URISyntaxException {
    java.net.URL url = JpetstoreApplication.class.getClassLoader().getResource(WEBAPP_RESOURCE);
    if (url == null) {
      throw new IllegalStateException("Web application resources '" + WEBAPP_RESOURCE + "' not found on class path");
    }
    if ("jar".equals(url.getProtocol())) {
      File jar = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      resources.addPreResources(new JarResourceSet(resources, "/", jar.getAbsolutePath(), "/" + WEBAPP_RESOURCE));
    } else {
      File dir = new File(url.toURI());
      resources.addPreResources(new DirResourceSet(resources, "/", dir.getAbsolutePath(), "/"));
    }
  }

  private static String normalizeContextPath(String path) {
    if (path == null || path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  private static String getSetting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
