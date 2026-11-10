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
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web
 * application (the former WAR content, now packaged in the JAR under {@code /webapp}).
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port to listen on (default {@code 8080}; set automatically by Cloud Run)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default {@code /jpetstore}; use empty value or {@code /} for
 * root)</li>
 * </ul>
 */
public final class Application {

  private static final String WEBAPP_RESOURCE_PATH = "/webapp";

  private Application() {
  }

  /**
   * Starts the embedded server and blocks until it is stopped.
   *
   * @param args
   *          command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    Tomcat tomcat = start(Integer.parseInt(getEnv("PORT", "8080")), getEnv("CONTEXT_PATH", "/jpetstore"));
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));
    tomcat.getServer().await();
  }

  /**
   * Creates and starts an embedded Tomcat instance.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" or "/" for root)
   *
   * @return the started Tomcat instance
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static Tomcat start(int port, String contextPath) throws Exception {
    String path = "/".equals(contextPath) ? "" : contextPath;

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("docbase"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(path, docBase.toString());
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    WebResourceRoot resources = new StandardRoot(context);
    addWebappResources(resources);
    context.setResources(resources);

    tomcat.start();
    return tomcat;
  }

  /**
   * Stops and destroys the given Tomcat instance.
   *
   * @param tomcat
   *          the instance to stop
   */
  public static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      System.err.println("Failed to stop embedded Tomcat: " + e.getMessage());
    }
  }

  private static void addWebappResources(WebResourceRoot resources) throws URISyntaxException {
    File location = new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // Running from the executable JAR
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_RESOURCE_PATH));
    } else {
      // Running from build output directory (e.g. target/classes)
      File webapp = new File(location, WEBAPP_RESOURCE_PATH);
      if (!webapp.isDirectory()) {
        webapp = new File("src/main/webapp");
      }
      resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
    }
  }

  private static String getEnv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
