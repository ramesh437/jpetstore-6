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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat and serves the web application packaged
 * under {@code /webapp} inside the JAR (or {@code target/classes/webapp} when started from an IDE).
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>{@code PORT} - HTTP port (default {@code 8080}; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default {@code /jpetstore}, use empty value for root)</li>
 * <li>{@code TOMCAT_BASE_DIR} - writable work directory (default: temporary directory)</li>
 * </ul>
 */
public final class JpetstoreServer {

  private static final String DEFAULT_PORT = "8080";
  private static final String DEFAULT_CONTEXT_PATH = "/jpetstore";
  private static final String WEBAPP_INTERNAL_PATH = "/webapp";

  private JpetstoreServer() {
    // entry point only
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
    String port = env("PORT", DEFAULT_PORT);
    Tomcat tomcat = createServer(Integer.parseInt(port.isEmpty() ? DEFAULT_PORT : port),
        env("CONTEXT_PATH", DEFAULT_CONTEXT_PATH));
    tomcat.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stopQuietly(tomcat), "jpetstore-shutdown"));
    tomcat.getServer().await();
  }

  /**
   * Creates (but does not start) an embedded Tomcat hosting the web application.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for root)
   * @return the configured Tomcat instance
   * @throws IOException
   *           if the work directory cannot be created
   * @throws URISyntaxException
   *           if the code source location cannot be resolved
   */
  public static Tomcat createServer(int port, String contextPath) throws IOException, URISyntaxException {
    String configuredBaseDir = System.getenv("TOMCAT_BASE_DIR");
    Path baseDir = configuredBaseDir == null || configuredBaseDir.trim().isEmpty()
        ? Files.createTempDirectory("jpetstore-tomcat")
        : Files.createDirectories(Paths.get(configuredBaseDir.trim()));
    Path docBase = Files.createDirectories(baseDir.resolve("webapp-root"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toAbsolutePath().toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getHost().setAutoDeploy(false);

    String path = normalizeContextPath(contextPath);
    Context context = tomcat.addWebapp(path, docBase.toAbsolutePath().toString());
    context.setParentClassLoader(JpetstoreServer.class.getClassLoader());

    File location = new File(JpetstoreServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    WebResourceRoot resources = new StandardRoot(context);
    if (location.isFile()) {
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_INTERNAL_PATH));
    } else {
      resources.addPreResources(new DirResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_INTERNAL_PATH));
    }
    context.setResources(resources);
    return tomcat;
  }

  private static String normalizeContextPath(String contextPath) {
    if (contextPath == null || contextPath.trim().isEmpty() || "/".equals(contextPath.trim())) {
      return "";
    }
    String value = contextPath.trim();
    return value.startsWith("/") ? value : "/" + value;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null ? defaultValue : value.trim();
  }

  private static void stopQuietly(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      // shutting down; nothing more to do
    }
  }
}
