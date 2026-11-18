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
package org.mybatis.jpetstore;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application
 * (previously packaged as a WAR). Suitable for GKE, Cloud Run and App Engine flexible environment.
 * <ul>
 * <li>PORT - HTTP port (default 8080; Cloud Run injects this variable)</li>
 * <li>CONTEXT_PATH - servlet context path (default "" i.e. root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private JpetstoreApplication() {
    // main class only
  }

  /**
   * Starts the embedded Tomcat server.
   *
   * @param args
   *          the command line arguments
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getEnv("PORT", "8080"));
    String contextPath = getEnv("CONTEXT_PATH", "");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getHost().setAppBase(baseDir.toString());

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    WebResourceRoot resources = new StandardRoot(context);
    addWebContent(resources);
    context.setResources(resources);
    if (context instanceof StandardContext) {
      // @WebListener classes are not discovered in a flat JAR, so register it explicitly
      context.addApplicationListener(StaxFactoryInitializationListener.class.getName());
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors on shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void addWebContent(WebResourceRoot resources) throws URISyntaxException {
    File location = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // running from the executable JAR: web content lives under META-INF/resources
      resources.addPreResources(
          new JarResourceSet(resources, "/", location.getAbsolutePath(), "/META-INF/resources"));
    } else {
      // running from classes directory (IDE / exploded build)
      File webapp = new File("src/main/webapp");
      if (webapp.isDirectory()) {
        resources.createWebResourceSet(ResourceSetType.PRE, "/", webapp.getAbsolutePath(), null, "/");
      }
    }
  }

  private static String getEnv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
