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
import java.security.CodeSource;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web
 * application (formerly packaged as a WAR), so the application can run on GKE, Cloud Run or App Engine flexible.
 *
 * <p>
 * Configuration is read from environment variables:
 * </p>
 * <ul>
 * <li>{@code PORT} - HTTP port (default: 8080, injected by Cloud Run)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default: empty, i.e. root)</li>
 * </ul>
 */
public final class Application {

  private static final String WEBAPP_RESOURCE_PATH = "/webapp";

  private Application() {
    // entry point only
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Starts the embedded server.
   *
   * @param args
   *          command line arguments (ignored)
   *
   * @throws Exception
   *           if the server cannot be started
   */
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
    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getService().addConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    Context context = addWebapp(tomcat, contextPath, baseDir);
    context.setParentClassLoader(Application.class.getClassLoader());

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

  private static Context addWebapp(Tomcat tomcat, String contextPath, Path baseDir)
      throws URISyntaxException, java.io.IOException {
    CodeSource codeSource = Application.class.getProtectionDomain().getCodeSource();
    File location = new File(codeSource.getLocation().toURI());

    if (location.isFile()) {
      // Running from the executable JAR: serve web content bundled under /webapp inside the JAR.
      Path docBase = Files.createDirectories(baseDir.resolve("webapp"));
      Context context = tomcat.addWebapp(contextPath, docBase.toString());
      WebResourceRoot resources = new StandardRoot(context);
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_RESOURCE_PATH));
      context.setResources(resources);
      return context;
    }

    // Running from an IDE / exploded classes directory: serve web content from the source tree.
    File webapp = new File("src/main/webapp");
    if (!webapp.isDirectory()) {
      webapp = new File(location, WEBAPP_RESOURCE_PATH.substring(1));
    }
    return tomcat.addWebapp(contextPath, webapp.getAbsolutePath());
  }
}
