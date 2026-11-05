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
import java.nio.file.Paths;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application, so the
 * application no longer has to be deployed as a WAR to an external application server.
 *
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>PORT (default: 8080) - HTTP port (Cloud Run / GKE inject this)</li>
 * <li>CONTEXT_PATH (default: empty = root context)</li>
 * <li>TOMCAT_BASE_DIR (default: a temporary directory) - scratch directory for Tomcat work files</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private EmbeddedServer() {
    // utility class
  }

  /**
   * Starts the embedded server and blocks until it is stopped.
   *
   * @param args
   *          command line arguments (ignored)
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = env("CONTEXT_PATH", "");
    Tomcat tomcat = create(port, contextPath);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));
    tomcat.start();
    tomcat.getServer().await();
  }

  /**
   * Creates (but does not start) an embedded Tomcat serving the web application.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for the root context)
   * @return the configured Tomcat instance
   * @throws Exception
   *           if the server cannot be configured
   */
  public static Tomcat create(int port, String contextPath) throws Exception {
    Tomcat tomcat = new Tomcat();

    String configuredBaseDir = System.getenv("TOMCAT_BASE_DIR");
    Path baseDir = configuredBaseDir == null || configuredBaseDir.trim().isEmpty()
        ? Files.createTempDirectory("jpetstore-tomcat")
        : Files.createDirectories(Paths.get(configuredBaseDir.trim()));
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Path location = codeSourceLocation();
    Context context;
    if (Files.isDirectory(location)) {
      // running from build output (e.g. integration tests): web resources are in <classes>/webapp
      context = tomcat.addWebapp(contextPath, location.resolve(WEBAPP_RESOURCE_DIR).toAbsolutePath().toString());
    } else {
      // running from the executable JAR: serve web resources directly from inside the JAR
      Path docBase = Files.createDirectories(baseDir.resolve("docbase"));
      context = tomcat.addWebapp(contextPath, docBase.toAbsolutePath().toString());
      WebResourceRoot resources = new StandardRoot(context);
      resources.addPostResources(
          new JarResourceSet(resources, "/", location.toAbsolutePath().toString(), "/" + WEBAPP_RESOURCE_DIR));
      context.setResources(resources);
    }
    context.setParentClassLoader(EmbeddedServer.class.getClassLoader());
    return tomcat;
  }

  /**
   * Stops and destroys the given server.
   *
   * @param tomcat
   *          the server to stop
   */
  public static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      throw new IllegalStateException("Failed to stop embedded Tomcat", e);
    }
  }

  private static Path codeSourceLocation() throws URISyntaxException {
    return new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
