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
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web
 * application (web.xml, JSPs and static resources are packaged inside the JAR under {@code /webapp}).
 *
 * <p>
 * Configuration (12-factor, via environment variables or system properties):
 * <ul>
 * <li>{@code PORT} - HTTP port (default 8080, Cloud Run / GKE compatible)</li>
 * <li>{@code CONTEXT_PATH} - context path (default {@code /jpetstore})</li>
 * <li>{@code WEBAPP_DIR} - optional directory containing the web application (used when not running from a JAR,
 * default {@code src/main/webapp})</li>
 * </ul>
 */
public final class Application {

  private static final String DEFAULT_PORT = "8080";
  private static final String DEFAULT_CONTEXT_PATH = "/jpetstore";
  private static final String EMBEDDED_WEBAPP_PATH = "/webapp";

  private Application() {
  }

  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.isEmpty() ? defaultValue : value;
  }

  /**
   * Start the embedded server and block until it is stopped.
   *
   * @param args
   *          command line arguments (ignored)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    Tomcat tomcat = start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        e.printStackTrace();
      }
    }));
    tomcat.getServer().await();
  }

  /**
   * Configure and start the embedded Tomcat.
   *
   * @return the started Tomcat instance
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static Tomcat start() throws Exception {
    int port = Integer.parseInt(setting("PORT", DEFAULT_PORT));
    String contextPath = setting("CONTEXT_PATH", DEFAULT_CONTEXT_PATH);
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
    context.setParentClassLoader(Application.class.getClassLoader());
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    WebResourceRoot resources = new StandardRoot(context);
    File location = locateCode();
    if (location.isFile()) {
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), EMBEDDED_WEBAPP_PATH));
    } else {
      File webappDir = new File(setting("WEBAPP_DIR", "src/main/webapp"));
      resources.addPreResources(new DirResourceSet(resources, "/", webappDir.getAbsolutePath(), "/"));
    }
    context.setResources(resources);

    tomcat.start();
    return tomcat;
  }

  private static File locateCode() throws URISyntaxException {
    return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }
}
