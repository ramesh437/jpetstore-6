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
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web content
 * (previously deployed as a WAR to an external application server), so the application can be run directly on GKE,
 * Cloud Run or App Engine flexible environment with <code>java -jar jpetstore.jar</code>.
 * <p>
 * Settings are read from environment variables (12-factor): <code>PORT</code> (default <code>8080</code>, injected by
 * Cloud Run; <code>SERVER_PORT</code> is used as fallback) and <code>CONTEXT_PATH</code> (default empty, i.e. root).
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_IN_JAR = "/webapp";

  private JpetstoreApplication() {
    // entry point only
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", env("SERVER_PORT", "8080")));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = tomcat.getConnector();
    connector.setPort(port);
    tomcat.getService().addConnector(connector);
    tomcat.getHost().setAppBase(baseDir.toString());
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setReloadable(false);
    configureResources(context, docBase);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void configureResources(Context context, Path docBase) throws IOException, URISyntaxException {
    WebResourceRoot resources = new StandardRoot(context);
    File location = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // Running from the executable JAR: serve the web content packaged under /webapp inside the JAR
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_IN_JAR));
    } else {
      // Running from an IDE / exploded classes: serve the sources directly
      Path sourceWebapp = Paths.get("src", "main", "webapp").toAbsolutePath();
      if (Files.isDirectory(sourceWebapp)) {
        resources.addPreResources(new DirResourceSet(resources, "/", sourceWebapp.toString(), "/"));
      }
    }
    context.setResources(resources);
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      System.err.println("Error while stopping embedded Tomcat: " + e.getMessage());
    }
  }

  private static String normalizeContextPath(String path) {
    if (path == null || path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
