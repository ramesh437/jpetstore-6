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
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application (JSPs,
 * static content and WEB-INF/web.xml are packaged below META-INF/resources) so that no external application server is
 * required (GKE, Cloud Run, App Engine flexible).
 *
 * <p>
 * Configuration through environment variables: PORT (default 8080, as injected by Cloud Run) and CONTEXT_PATH (default
 * empty = root context).
 */
public final class Application {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private Application() {
    // main class only
  }

  /**
   * Starts the embedded server.
   *
   * @param args
   *          the command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", env("SERVER_PORT", "8080")));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(Application.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File location = codeSourceLocation();
    if (location.isDirectory()) {
      resources.addPreResources(new DirResourceSet(resources, "/", location.getAbsolutePath(), WEB_RESOURCES_PATH));
    } else {
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEB_RESOURCES_PATH));
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      // shutting down anyway
    }
  }

  private static File codeSourceLocation() throws URISyntaxException {
    return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String env(String name, String defaultValue) {
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
