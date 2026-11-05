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
 * Executable entry point that runs JPetStore on an embedded Tomcat server, so the application can be shipped as a
 * self-contained JAR (GKE, Cloud Run, App Engine flexible) instead of a WAR deployed to an external application server.
 *
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP listen port (default {@code 8080}; Cloud Run injects this automatically)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default empty, i.e. root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private JpetstoreApplication() {
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = env("CONTEXT_PATH", "");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    baseDir.toFile().deleteOnExit();

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Path docBase = Files.createDirectory(baseDir.resolve("docbase"));
    Context context = tomcat.addWebapp(contextPath, docBase.toString());

    // Serve the web application content (WEB-INF, JSPs, static files) packaged inside the executable JAR.
    WebResourceRoot resources = new StandardRoot(context);
    File codeSource = codeSource();
    if (codeSource.isFile()) {
      resources.addPreResources(new JarResourceSet(resources, "/", codeSource.getAbsolutePath(), "/" + WEBAPP_RESOURCE_DIR));
    } else {
      resources.addPreResources(new DirResourceSet(resources, "/", new File(codeSource, WEBAPP_RESOURCE_DIR).getAbsolutePath(), "/"));
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // best effort on shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File codeSource() throws URISyntaxException {
    return new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
