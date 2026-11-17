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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Executable entry point that runs JPetStore on an embedded Tomcat (self-contained JAR, suitable for GKE, Cloud Run
 * and App Engine flexible environment).
 *
 * <p>
 * Runtime configuration (12-factor):
 * <ul>
 * <li>PORT - HTTP port (default 8080; Cloud Run injects this automatically)</li>
 * <li>CONTEXT_PATH - web application context path (default /jpetstore)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_PATH = "/webapp";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Start the embedded server and block until it is stopped.
   *
   * @param args
   *          command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    Tomcat tomcat = start(resolvePort(), resolveContextPath());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // best effort on shutdown
      }
    }));
    tomcat.getServer().await();
  }

  /**
   * Create and start an embedded Tomcat serving the bundled web application.
   *
   * @param port
   *          http port
   * @param contextPath
   *          context path (empty string or "/" for root)
   *
   * @return the started Tomcat instance
   *
   * @throws IOException
   *           if the temporary base directory cannot be created
   * @throws LifecycleException
   *           if the server fails to start
   * @throws URISyntaxException
   *           if the code source location cannot be resolved
   */
  public static Tomcat start(int port, String contextPath) throws IOException, LifecycleException, URISyntaxException {
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File codeSource = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    WebResourceSet webContent;
    if (codeSource.isDirectory()) {
      webContent = new DirResourceSet(resources, "/", codeSource.getAbsolutePath(), WEBAPP_RESOURCE_PATH);
    } else {
      webContent = new JarResourceSet(resources, "/", codeSource.getAbsolutePath(), WEBAPP_RESOURCE_PATH);
    }
    resources.addPreResources(webContent);
    context.setResources(resources);

    tomcat.start();
    return tomcat;
  }

  private static int resolvePort() {
    String port = System.getenv("PORT");
    if (port == null || port.trim().isEmpty()) {
      return 8080;
    }
    return Integer.parseInt(port.trim());
  }

  private static String resolveContextPath() {
    String path = System.getenv("CONTEXT_PATH");
    if (path == null) {
      return "/jpetstore";
    }
    path = path.trim();
    return "/".equals(path) ? "" : path;
  }
}
