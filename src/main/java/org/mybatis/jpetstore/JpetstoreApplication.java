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
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application, so the
 * application can be run directly on GKE, Cloud Run or App Engine flexible environment with {@code java -jar}.
 * <p>
 * Configuration is read from environment variables (12-factor): {@code PORT} (default 8080) and {@code CONTEXT_PATH}
 * (default root context).
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Starts the embedded server and blocks until it is stopped.
   *
   * @param args
   *          the command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getenv("PORT", "8080"));
    String contextPath = getenv("CONTEXT_PATH", "");

    Tomcat tomcat = start(port, contextPath);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        e.printStackTrace();
      }
    }, "tomcat-shutdown"));
    tomcat.getServer().await();
  }

  /**
   * Creates and starts an embedded Tomcat serving the web application.
   *
   * @param port
   *          the HTTP port ({@code 0} selects a random free port)
   * @param contextPath
   *          the context path ("" for the root context)
   *
   * @return the started Tomcat instance
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static Tomcat start(int port, String contextPath) throws Exception {
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapps").resolve("ROOT"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    WebResourceRoot resources = new StandardRoot(context);
    addWebappResources(resources);
    context.setResources(resources);

    tomcat.start();
    return tomcat;
  }

  private static void addWebappResources(WebResourceRoot resources) throws URISyntaxException {
    File location = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // Running from the executable JAR: web content is packaged under /webapp
      resources
          .addJarResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), "/" + WEBAPP_RESOURCE_DIR));
    } else {
      // Running from the build output directory (IDE / tests)
      File webapp = new File(location, WEBAPP_RESOURCE_DIR);
      if (!webapp.isDirectory()) {
        webapp = new File("src/main/webapp");
      }
      resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
    }
  }

  private static String getenv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
