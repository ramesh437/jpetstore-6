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
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web
 * application (previously deployed as a WAR to an external application server).
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>{@code PORT} (or {@code SERVER_PORT}) - HTTP port, defaults to 8080 (Cloud Run / GKE compatible)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path, defaults to the root context</li>
 * </ul>
 */
public final class EmbeddedTomcatServer {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private EmbeddedTomcatServer() {
    // entry point only
  }

  /**
   * Starts the embedded server.
   *
   * @param args
   *          the command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", env("SERVER_PORT", "8080")));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());

    // Serve the web resources (JSPs, WEB-INF/web.xml, static files) packaged inside this JAR.
    WebResourceRoot resources = new StandardRoot(context);
    resources.createWebResourceSet(ResourceSetType.PRE, "/", locateApplicationArchive(), null, WEB_RESOURCES_PATH);
    context.setResources(resources);

    // @WebListener classes inside the application JAR are not discovered by Tomcat's annotation scanning.
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // best effort on shutdown
      }
    }, "tomcat-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static String locateApplicationArchive() throws URISyntaxException {
    return new File(EmbeddedTomcatServer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
        .getAbsolutePath();
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  private static String normalizeContextPath(String path) {
    if (path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }
}
