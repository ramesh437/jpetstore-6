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
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application (web
 * resources are packaged under {@code META-INF/resources}), so no external application server / WAR deployment is
 * required (GKE, Cloud Run, App Engine flexible).
 * <p>
 * Configuration (12-factor) via environment variables:
 * <ul>
 * <li>{@code PORT} - HTTP listen port (default 8080, injected by Cloud Run)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default "" = root)</li>
 * </ul>
 */
public final class JpetstoreServer {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private JpetstoreServer() {
  }

  /**
   * Start the embedded server.
   *
   * @param args
   *          command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getenv("PORT", "8080"));
    String contextPath = getenv("CONTEXT_PATH", "");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.getService().addConnector(connector);
    tomcat.setConnector(connector);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreServer.class.getClassLoader());
    context.setReloadable(false);

    WebResourceRoot resources = new StandardRoot(context);
    File codeSource = codeSourceLocation();
    if (codeSource.isFile()) {
      resources.addPreResources(new JarResourceSet(resources, "/", codeSource.getAbsolutePath(), WEB_RESOURCES_PATH));
    } else {
      resources.addPreResources(new DirResourceSet(resources, "/", codeSource.getAbsolutePath(), WEB_RESOURCES_PATH));
    }
    context.setResources(resources);

    // Graceful shutdown on SIGTERM (Cloud Run / GKE)
    Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(tomcat), "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File codeSourceLocation() throws URISyntaxException {
    return new File(JpetstoreServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static void shutdown(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      // nothing else can be done during JVM shutdown
      e.printStackTrace();
    }
  }

  private static String getenv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

}
