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

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application
 * (Spring MVC + JSP) packaged under {@code META-INF/resources}, replacing the deployment of a WAR file to an external
 * application server.
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port (default: 8080; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - context path of the application (default: /jpetstore, use empty or / for the root)</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEB_RESOURCES_BASE = "/META-INF/resources";

  private EmbeddedServer() {
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", "/jpetstore"));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(EmbeddedServer.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File location = locationOfApplication();
    if (location.isDirectory()) {
      resources.addPreResources(
          new DirResourceSet(resources, "/", new File(location, WEB_RESOURCES_BASE).getAbsolutePath(), "/"));
    } else {
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEB_RESOURCES_BASE));
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // ignore errors on shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File locationOfApplication() throws URISyntaxException {
    return new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String normalizeContextPath(String value) {
    if (value == null || value.isEmpty() || "/".equals(value)) {
      return "";
    }
    return value.startsWith("/") ? value : "/" + value;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
