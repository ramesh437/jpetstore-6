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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained (executable JAR) JPetStore distribution. It starts an embedded Apache Tomcat that
 * serves the web application, so no external application server is required (e.g. for GKE, Cloud Run or App Engine
 * flexible environment).
 *
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>{@code PORT} - HTTP port (default {@code 8080}; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - web application context path (default empty = root context)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_ROOT = "webapp";

  private JpetstoreApplication() {
    // entry point only
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
    int port = Integer.parseInt(getenv("PORT", "8080"));
    String contextPath = normalizeContextPath(getenv("CONTEXT_PATH", ""));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = extractWebapp(baseDir);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getHost().setAppBase(baseDir.toString());

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());
    // Replaces the former @WebListener scanning, which does not apply to classes on the application classpath
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // ignore errors during shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static String getenv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  private static String normalizeContextPath(String path) {
    if ("/".equals(path) || path.isEmpty()) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  /**
   * Resolves the web application directory that is packaged on the classpath under {@code webapp/}. When running from
   * the executable JAR the content is extracted into a temporary directory.
   */
  private static Path extractWebapp(Path baseDir) throws IOException, URISyntaxException {
    URL location = JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation();
    Path source = Paths.get(location.toURI());
    if (Files.isDirectory(source)) {
      // running from exploded classes (IDE / target/classes)
      return source.resolve(WEBAPP_RESOURCE_ROOT);
    }
    Path target = baseDir.resolve("webapp");
    Files.createDirectories(target);
    String prefix = WEBAPP_RESOURCE_ROOT + "/";
    try (JarFile jar = new JarFile(source.toFile())) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
          continue;
        }
        Path out = target.resolve(name.substring(prefix.length())).normalize();
        if (!out.startsWith(target)) {
          continue;
        }
        if (entry.isDirectory()) {
          Files.createDirectories(out);
        } else {
          Files.createDirectories(out.getParent());
          try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }
    return target;
  }
}
