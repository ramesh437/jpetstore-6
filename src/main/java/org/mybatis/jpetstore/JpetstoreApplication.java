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
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point that runs JPetStore on an embedded Tomcat so that the application can be shipped as a self-contained
 * executable JAR (e.g. for GKE / Cloud Run).
 *
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} / {@code SERVER_PORT} - HTTP port (default 8080, Cloud Run injects {@code PORT})</li>
 * <li>{@code CONTEXT_PATH} - web application context path (default "" i.e. root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_PATH = "/META-INF/resources";

  private JpetstoreApplication() {
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(firstNonBlank(System.getenv("PORT"), System.getenv("SERVER_PORT"), "8080"));
    String contextPath = firstNonBlank(System.getenv("CONTEXT_PATH"), "");
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getHost().setAppBase(baseDir.toString());
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File location = locateCodeSource();
    if (location.isFile()) {
      // Running from the executable (shaded) JAR: serve web content from META-INF/resources inside the JAR
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_RESOURCE_PATH));
    } else {
      // Running from an IDE / exploded classes directory
      File webapp = new File("src/main/webapp");
      if (webapp.isDirectory()) {
        resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
      } else {
        resources.addPreResources(new DirResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_RESOURCE_PATH));
      }
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors during shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File locateCodeSource() throws URISyntaxException {
    return new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        return value.trim();
      }
    }
    return "";
  }
}
