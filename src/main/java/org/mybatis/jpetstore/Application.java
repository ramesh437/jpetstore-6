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
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Executable entry point that starts the application on an embedded Tomcat, so that it can be run as a self-contained
 * JAR (e.g. on GKE, Cloud Run or App Engine flexible environment).
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>{@code PORT} (or {@code SERVER_PORT}) - HTTP port, default {@code 8080}</li>
 * <li>{@code CONTEXT_PATH} - servlet context path, default {@code /jpetstore}; use an empty value for the root
 * context</li>
 * </ul>
 */
public final class Application {

  private static final String DEFAULT_PORT = "8080";
  private static final String DEFAULT_CONTEXT_PATH = "/jpetstore";
  private static final String WEB_RESOURCES_IN_JAR = "/META-INF/resources";

  private Application() {
  }

  /**
   * Starts the embedded server.
   *
   * @param args
   *          command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(firstNonBlank(System.getenv("PORT"), System.getenv("SERVER_PORT"), DEFAULT_PORT));
    String contextPath = normalizeContextPath(System.getenv("CONTEXT_PATH"));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    baseDir.toFile().deleteOnExit();

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());

    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.getService().addConnector(connector);
    tomcat.setConnector(connector);

    File location = codeSourceLocation();
    StandardContext context;
    if (location.isFile()) {
      // Running from the executable JAR: web resources are packaged under META-INF/resources.
      Path docBase = Files.createDirectories(baseDir.resolve("webapp"));
      context = (StandardContext) tomcat.addWebapp(contextPath, docBase.toString());
      WebResourceRoot resources = new StandardRoot(context);
      resources.addPostResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEB_RESOURCES_IN_JAR));
      context.setResources(resources);
    } else {
      // Running from an IDE / exploded classes directory.
      File webapp = new File("src/main/webapp");
      context = (StandardContext) tomcat.addWebapp(contextPath, webapp.getAbsolutePath());
      WebResourceRoot resources = new StandardRoot(context);
      resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", location.getAbsolutePath(), "/"));
      context.setResources(resources);
    }
    context.setParentClassLoader(Application.class.getClassLoader());
    configure(context);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // ignore errors during shutdown
      }
    }, "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void configure(Context context) {
    context.setReloadable(false);
  }

  private static File codeSourceLocation() throws URISyntaxException {
    return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String normalizeContextPath(String value) {
    String path = value == null ? DEFAULT_CONTEXT_PATH : value.trim();
    if (path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        return value.trim();
      }
    }
    return null;
  }
}
