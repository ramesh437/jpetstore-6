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
 * Executable entry point that runs JPetStore on an embedded Tomcat (self-contained JAR, no external application server
 * required), suitable for GKE, Cloud Run or App Engine flexible.
 * <p>
 * Configuration is read from environment variables:
 * <ul>
 * <li>{@code PORT} (default {@code 8080}) - HTTP port (Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} (default {@code /jpetstore}) - web application context path ({@code /} or empty for
 * root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Starts the embedded server and blocks until it is stopped.
   *
   * @param args
   *          the command line arguments (ignored)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    Tomcat tomcat = start(Integer.parseInt(env("PORT", "8080")), env("CONTEXT_PATH", "/jpetstore"));
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // ignore errors on shutdown
      }
    }));
    tomcat.getServer().await();
  }

  /**
   * Starts an embedded Tomcat serving the packaged web application.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path
   *
   * @return the started server
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static Tomcat start(int port, String contextPath) throws Exception {
    String path = "/".equals(contextPath) ? "" : contextPath;

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    baseDir.toFile().deleteOnExit();

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Path docBase = Files.createDirectory(baseDir.resolve("webapp"));
    Context context = tomcat.addWebapp(path, docBase.toString());

    File codeSource = codeSource();
    WebResourceRoot resources = new StandardRoot(context);
    if (codeSource.isDirectory()) {
      resources.addPreResources(
          new DirResourceSet(resources, "/", new File(codeSource, WEB_RESOURCES_PATH).getPath(), "/"));
    } else {
      resources.addPreResources(new JarResourceSet(resources, "/", codeSource.getPath(), WEB_RESOURCES_PATH));
    }
    context.setResources(resources);

    tomcat.start();
    return tomcat;
  }

  private static File codeSource() throws URISyntaxException {
    return new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
