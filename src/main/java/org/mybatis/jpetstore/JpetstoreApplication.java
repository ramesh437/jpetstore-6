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
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
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
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application packaged
 * under <code>webapp/</code> inside the JAR, so no external application server (WAR deployment) is required. Suitable
 * for GKE, Cloud Run and App Engine flexible environment.
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>PORT - HTTP listen port (default: 8080, Cloud Run injects this)</li>
 * <li>CONTEXT_PATH - servlet context path (default: "" i.e. root)</li>
 * <li>TOMCAT_BASE_DIR - writable work directory (default: java.io.tmpdir)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_MARKER = "webapp/WEB-INF/web.xml";

  private JpetstoreApplication() {
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
    Tomcat tomcat = createServer(getPort(), getContextPath());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));
    tomcat.start();
    tomcat.getServer().await();
  }

  /**
   * Creates (but does not start) the embedded Tomcat.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for root)
   *
   * @return the configured Tomcat instance
   *
   * @throws IOException
   *           if the work directory or web resources cannot be resolved
   */
  public static Tomcat createServer(int port, String contextPath) throws IOException {
    String baseDirEnv = System.getenv("TOMCAT_BASE_DIR");
    Path baseDir = baseDirEnv == null || baseDirEnv.isEmpty() ? Files.createTempDirectory("jpetstore-tomcat")
        : Files.createDirectories(new File(baseDirEnv).toPath());

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toAbsolutePath().toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Path docBase = Files.createTempDirectory(baseDir, "docbase");
    Context context = tomcat.addWebapp(contextPath, docBase.toAbsolutePath().toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    URL marker = JpetstoreApplication.class.getClassLoader().getResource(WEBAPP_MARKER);
    if (marker == null) {
      throw new IllegalStateException("Web application resources not found on classpath: " + WEBAPP_MARKER);
    }
    if ("jar".equals(marker.getProtocol())) {
      File jar = new File(URLDecoder.decode(((JarURLConnection) marker.openConnection()).getJarFileURL().getPath(), "UTF-8"));
      resources.addPreResources(new JarResourceSet(resources, "/", jar.getAbsolutePath(), "/webapp"));
    } else {
      File webappDir = new File(URLDecoder.decode(marker.getPath(), "UTF-8")).getParentFile().getParentFile();
      resources.addPreResources(new DirResourceSet(resources, "/", webappDir.getAbsolutePath(), "/"));
    }
    context.setResources(resources);
    return tomcat;
  }

  private static int getPort() {
    String port = System.getenv("PORT");
    return port == null || port.isEmpty() ? 8080 : Integer.parseInt(port);
  }

  private static String getContextPath() {
    String path = System.getenv("CONTEXT_PATH");
    if (path == null || path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      e.printStackTrace();
    }
  }
}
