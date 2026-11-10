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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Entry point of the self-contained (executable JAR) JPetStore application. It starts an embedded Tomcat that serves
 * the web content bundled in the JAR (classpath folder {@code webapp}), so no external application server is needed
 * (GKE, Cloud Run, App Engine flexible environment).
 *
 * <p>
 * Configuration is read from environment variables (or JVM system properties with the same name):
 * <ul>
 * <li>{@code PORT} - HTTP port (default {@code 8080}, injected automatically by Cloud Run)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default empty, i.e. root)</li>
 * <li>{@code TOMCAT_BASE_DIR} - Tomcat working directory (default: a temporary directory)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEB_XML_RESOURCE = "webapp/WEB-INF/web.xml";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Starts the application and blocks until the server is stopped.
   *
   * @param args
   *          the command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
    int port = Integer.parseInt(getSetting("PORT", "8080"));
    String contextPath = getSetting("CONTEXT_PATH", "");

    Tomcat tomcat = createServer(port, contextPath);
    // Graceful shutdown on SIGTERM (sent by Kubernetes / Cloud Run)
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stopQuietly(tomcat), "tomcat-shutdown"));
    tomcat.start();
    tomcat.getServer().await();
  }

  /**
   * Creates (but does not start) an embedded Tomcat serving the JPetStore web application.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for root)
   *
   * @return the configured Tomcat instance
   *
   * @throws IOException
   *           if the working directories cannot be created
   */
  public static Tomcat createServer(int port, String contextPath) throws IOException {
    Tomcat tomcat = new Tomcat();
    Path baseDir = resolveBaseDir();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Path docBase = Files.createDirectories(baseDir.resolve("docbase"));
    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    resources.addPreResources(createWebContentResourceSet(resources));
    context.setResources(resources);
    return tomcat;
  }

  /**
   * Stops the given Tomcat instance, ignoring errors.
   *
   * @param tomcat
   *          the instance to stop
   */
  public static void stopQuietly(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      // ignore on shutdown
    }
  }

  private static org.apache.catalina.WebResourceSet createWebContentResourceSet(WebResourceRoot resources)
      throws IOException {
    URL webXml = JpetstoreApplication.class.getClassLoader().getResource(WEB_XML_RESOURCE);
    if (webXml == null) {
      throw new IllegalStateException("Web content not found on the classpath: " + WEB_XML_RESOURCE);
    }
    try {
      if ("jar".equals(webXml.getProtocol())) {
        JarURLConnection connection = (JarURLConnection) webXml.openConnection();
        String jarFile = Paths.get(connection.getJarFileURL().toURI()).toString();
        return new JarResourceSet(resources, "/", jarFile, "/webapp");
      }
      // running from exploded classes (IDE / integration tests)
      Path webAppDir = Paths.get(webXml.toURI()).getParent().getParent();
      return new DirResourceSet(resources, "/", webAppDir.toString(), "/");
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot resolve web content location: " + webXml, e);
    }
  }

  private static Path resolveBaseDir() throws IOException {
    String configured = getSetting("TOMCAT_BASE_DIR", null);
    if (configured != null) {
      return Files.createDirectories(new File(configured).toPath());
    }
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    baseDir.toFile().deleteOnExit();
    return baseDir;
  }

  private static String getSetting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
