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
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat which serves the web content packaged in
 * the JAR (under <code>/webapp</code>) using the standard <code>WEB-INF/web.xml</code> deployment descriptor.
 * <p>
 * Configuration is read from the environment (12-factor):
 * <ul>
 * <li><code>PORT</code> - HTTP port (default 8080, injected by Cloud Run / GKE)</li>
 * <li><code>CONTEXT_PATH</code> - servlet context path (default empty = root)</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEBAPP_PATH = "/webapp";

  private final Tomcat tomcat;

  private EmbeddedServer(Tomcat tomcat) {
    this.tomcat = tomcat;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Start the embedded server.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for root)
   *
   * @return the running server
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static EmbeddedServer start(int port, String contextPath) throws Exception {
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    baseDir.toFile().deleteOnExit();

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());

    Connector connector = new Connector("HTTP/1.1");
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, baseDir.toString());
    WebResourceRoot resources = new StandardRoot(context);
    File location = new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isDirectory()) {
      resources.addPreResources(new DirResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_PATH));
    } else {
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_PATH));
    }
    context.setResources(resources);
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    tomcat.start();
    return new EmbeddedServer(tomcat);
  }

  /**
   * Stop the embedded server.
   *
   * @throws LifecycleException
   *           if the server cannot be stopped
   */
  public void stop() throws LifecycleException {
    tomcat.stop();
    tomcat.destroy();
  }

  private void await() {
    tomcat.getServer().await();
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
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = env("CONTEXT_PATH", "");
    EmbeddedServer server = start(port, contextPath);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        server.stop();
      } catch (LifecycleException e) {
        // ignore errors during shutdown
      }
    }, "jpetstore-shutdown"));
    server.await();
  }

}
