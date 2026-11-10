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
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web application
 * (the content of {@code src/main/webapp} is packaged on the classpath under {@code /webapp}), so no external
 * application server or WAR deployment is required (GKE / Cloud Run / App Engine flexible).
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port to listen on (default {@code 8080}; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default {@code /jpetstore}, use empty value for the root)</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEBAPP_CLASSPATH_ROOT = "/webapp";

  private EmbeddedServer() {
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
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = System.getenv("CONTEXT_PATH") == null ? "/jpetstore" : System.getenv("CONTEXT_PATH").trim();
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(EmbeddedServer.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    resources.createWebResourceSet(WebResourceRoot.ResourceSetType.POST, "/", locateClasspathRoot(), null,
        WEBAPP_CLASSPATH_ROOT);
    context.setResources(resources);

    // Annotation scanning (@WebListener) is not performed for classes outside of WEB-INF, register explicitly.
    context.addApplicationListener("org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener");

    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
    stop(tomcat);
  }

  private static String locateClasspathRoot() throws URISyntaxException {
    return new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
        .getAbsolutePath();
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      // nothing more can be done while shutting down
    }
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
