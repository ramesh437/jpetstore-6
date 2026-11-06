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
 * (JSPs, static resources and WEB-INF/web.xml) packaged inside the JAR.
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port (default 8080, as injected by Cloud Run / GKE)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default empty, i.e. root)</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEB_CONTENT_IN_JAR = "/META-INF/resources";
  private static final String WEB_CONTENT_IN_SOURCE_TREE = "src/main/webapp";

  private EmbeddedServer() {
  }

  public static void main(String[] args) throws LifecycleException, IOException, URISyntaxException {
    int port = Integer.parseInt(getEnv("PORT", "8080"));
    String contextPath = normalizeContextPath(getEnv("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    baseDir.toFile().deleteOnExit();
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getService().addConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    WebResourceRoot resources = new StandardRoot(context);
    addWebContent(resources);
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // ignore errors during shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void addWebContent(WebResourceRoot resources) throws URISyntaxException {
    File location = new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // Running from the executable JAR: web content lives under META-INF/resources
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEB_CONTENT_IN_JAR));
    } else {
      // Running from the IDE / target/classes
      File webapp = new File(WEB_CONTENT_IN_SOURCE_TREE);
      resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
    }
  }

  private static String getEnv(String name, String defaultValue) {
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
