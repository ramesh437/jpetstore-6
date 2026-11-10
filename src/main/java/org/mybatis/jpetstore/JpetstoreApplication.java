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
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web application
 * (web.xml, Spring contexts and JSPs are packaged inside the JAR under {@code /webapp}).
 * <p>
 * Runtime settings are read from environment variables (12-factor):
 * <ul>
 * <li>{@code PORT} - HTTP port to listen on (default {@code 8080}; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default empty, i.e. root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private JpetstoreApplication() {
  }

  /**
   * Starts the embedded servlet container.
   *
   * @param args
   *          the command line arguments (not used)
   *
   * @throws Exception
   *           if the container cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("docbase"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = new Connector("HTTP/1.1");
    connector.setPort(port);
    connector.setURIEncoding("UTF-8");
    tomcat.setConnector(connector);
    tomcat.getService().addConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    addWebappResources(resources);
    context.setResources(resources);
    // Equivalent of the @WebListener annotation (annotation scanning is not applied to the embedded classpath)
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        e.printStackTrace();
      } finally {
        deleteQuietly(baseDir);
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void addWebappResources(WebResourceRoot resources) throws URISyntaxException {
    File location = new File(
        JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      // running from the executable JAR
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), "/" + WEBAPP_RESOURCE_DIR));
    } else {
      // running from an IDE / exploded classes directory
      File webapp = new File(location, WEBAPP_RESOURCE_DIR);
      if (!webapp.isDirectory()) {
        webapp = new File("src/main/webapp");
      }
      resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
    }
  }

  private static String normalizeContextPath(String value) {
    if (value.isEmpty() || "/".equals(value)) {
      return "";
    }
    return value.startsWith("/") ? value : "/" + value;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  private static void deleteQuietly(Path dir) {
    try (Stream<Path> paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    } catch (IOException e) {
      // ignore
    }
  }
}
