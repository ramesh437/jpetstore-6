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

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web application
 * (Spring MVC + JSP) so no external application server (WAR deployment) is required.
 * <p>
 * Runtime settings (12-factor, read from environment variables or system properties):
 * <ul>
 * <li>PORT (default: 8080) - HTTP port, e.g. injected by Cloud Run</li>
 * <li>CONTEXT_PATH (default: empty / root context)</li>
 * <li>BIND_ADDRESS (default: 0.0.0.0)</li>
 * <li>WEBAPP_DIR (optional) - exploded web application directory, used when not running from a JAR</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String JAR_WEB_RESOURCES_PATH = "/META-INF/resources";

  private EmbeddedServer() {
    // utility entry point
  }

  /**
   * Application entry point.
   *
   * @param args
   *          command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getSetting("PORT", "8080"));
    String contextPath = normalizeContextPath(getSetting("CONTEXT_PATH", ""));
    String bindAddress = getSetting("BIND_ADDRESS", "0.0.0.0");

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectory(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());

    Connector connector = new Connector();
    connector.setPort(port);
    connector.setProperty("address", bindAddress);
    tomcat.setConnector(connector);
    tomcat.getService().addConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setResources(createResources(context));

    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static WebResourceRoot createResources(Context context) throws URISyntaxException {
    WebResourceRoot resources = new StandardRoot(context);
    File location = new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // Running from the executable JAR: web resources live under META-INF/resources
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), JAR_WEB_RESOURCES_PATH));
    } else {
      // Running from an IDE / exploded classes: use the source web application directory
      File webappDir = new File(getSetting("WEBAPP_DIR", "src/main/webapp"));
      resources.addPreResources(new DirResourceSet(resources, "/", webappDir.getAbsolutePath(), "/"));
    }
    return resources;
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      System.err.println("Error while stopping Tomcat: " + e.getMessage());
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
