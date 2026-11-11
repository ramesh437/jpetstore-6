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
package org.mybatis.jpetstore.web;

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
 * Entry point that runs JPetStore as a self-contained executable JAR with an embedded Tomcat, so no external
 * application server (WAR deployment) is required. Suitable for GKE, Cloud Run and App Engine flexible environment.
 * <p>
 * Settings come from environment variables:
 * <ul>
 * <li>{@code PORT} - HTTP port to listen on (default {@code 8080}; Cloud Run injects this automatically)</li>
 * <li>{@code CONTEXT_PATH} - web application context path (default {@code /jpetstore}; use {@code /} for root)</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEBAPP_RESOURCE_PATH = "/webapp";
  private static final String SOURCE_WEBAPP_DIR = "src/main/webapp";

  private EmbeddedServer() {
    // main class only
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Starts the embedded Tomcat server.
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
    } else if (!contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    WebResourceRoot resources = new StandardRoot(context);
    addWebappResources(resources);
    context.setResources(resources);
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void addWebappResources(WebResourceRoot resources) throws URISyntaxException {
    File location = new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // running from the executable JAR: web resources are packaged under /webapp
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_RESOURCE_PATH));
      return;
    }
    // running from an IDE / exploded classes directory
    File sourceWebapp = new File(SOURCE_WEBAPP_DIR);
    File webapp = sourceWebapp.isDirectory() ? sourceWebapp : new File(location, "webapp");
    resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      System.err.println("Error while stopping embedded Tomcat: " + e.getMessage());
    }
  }
}
