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

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application bundled
 * inside the JAR (under {@code /webapp}), so no external application server is required (GKE, Cloud Run, App Engine
 * flexible).
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port (default 8080; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default {@code /jpetstore}; use an empty value for the root)</li>
 * </ul>
 */
public final class EmbeddedTomcatServer {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private EmbeddedTomcatServer() {
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null ? defaultValue : value.trim();
  }

  public static void main(String[] args) throws IOException, LifecycleException, URISyntaxException {
    String portValue = env("PORT", "8080");
    int port = Integer.parseInt(portValue.isEmpty() ? "8080" : portValue);
    String contextPath = env("CONTEXT_PATH", "/jpetstore");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }

    File baseDir = Files.createTempDirectory("jpetstore-tomcat").toFile();
    baseDir.deleteOnExit();
    File docBase = new File(baseDir, "docbase");
    if (!docBase.mkdirs() && !docBase.isDirectory()) {
      throw new IOException("Unable to create " + docBase);
    }

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.getAbsolutePath());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.getAbsolutePath());
    context.setParentClassLoader(EmbeddedTomcatServer.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    resources.addPreResources(webappResources(resources));
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        e.printStackTrace();
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static WebResourceSet webappResources(WebResourceRoot root) throws URISyntaxException {
    File location = new File(
        EmbeddedTomcatServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // Running from the executable JAR: serve the bundled /webapp folder.
      return new JarResourceSet(root, "/", location.getAbsolutePath(), "/" + WEBAPP_RESOURCE_DIR);
    }
    // Running from an IDE / exploded classes directory.
    File dir = new File(location, WEBAPP_RESOURCE_DIR);
    if (!dir.isDirectory()) {
      dir = new File("src/main/webapp");
    }
    return new DirResourceSet(root, "/", dir.getAbsolutePath(), "/");
  }
}
