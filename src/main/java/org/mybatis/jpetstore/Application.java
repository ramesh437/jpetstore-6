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
 * Entry point of the self-contained (executable JAR) JPetStore application. Starts an embedded Tomcat that serves the
 * web content packaged under {@code META-INF/resources} (Spring MVC / Spring Session configuration is still driven by
 * {@code WEB-INF/web.xml}).
 * <p>
 * Configuration (12-factor, environment variables or system properties):
 * <ul>
 * <li>PORT (default 8080) - HTTP port (injected by Cloud Run / GKE)</li>
 * <li>CONTEXT_PATH (default empty, i.e. root context)</li>
 * </ul>
 */
public final class Application {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private Application() {
  }

  /**
   * Starts the application and blocks until the server is stopped.
   *
   * @param args
   *          command line arguments (not used)
   *
   * @throws Exception
   *           if the server could not be started
   */
  public static void main(String[] args) throws Exception {
    Tomcat tomcat = start(Integer.parseInt(setting("PORT", "8080")), setting("CONTEXT_PATH", ""));
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));
    tomcat.getServer().await();
  }

  /**
   * Starts an embedded Tomcat instance.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for root)
   *
   * @return the started server
   *
   * @throws Exception
   *           if the server could not be started
   */
  public static Tomcat start(int port, String contextPath) throws Exception {
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector().setProperty("address", "0.0.0.0");
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(normalize(contextPath), docBase.toString());
    context.setParentClassLoader(Application.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File location = codeSource();
    if (location.isDirectory()) {
      resources.addPreResources(
          new DirResourceSet(resources, "/", new File(location, WEB_RESOURCES_PATH).getAbsolutePath(), "/"));
    } else {
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), WEB_RESOURCES_PATH));
    }
    context.setResources(resources);

    tomcat.start();
    return tomcat;
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      e.printStackTrace();
    }
  }

  private static File codeSource() throws URISyntaxException {
    return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String normalize(String contextPath) {
    if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
      return "";
    }
    return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
  }

  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
