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
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Executable entry point that runs JPetStore on an embedded Tomcat, so the application is shipped as a self-contained
 * JAR ({@code java -jar jpetstore.jar}) instead of a WAR deployed to an external application server.
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>PORT - HTTP port to listen on (default: 8080; Cloud Run / GKE inject this)</li>
 * <li>CONTEXT_PATH - servlet context path (default: /jpetstore, use an empty value or "/" for the root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_ROOT = "jpetstore-webapp";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Starts the embedded Tomcat.
   *
   * @param args
   *          the command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", "/jpetstore"));

    Path workDir = Files.createTempDirectory("jpetstore");
    Path docBase = workDir.resolve("webapp");
    Files.createDirectories(docBase);
    extractWebapp(docBase);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(workDir.resolve("tomcat").toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    // Classpath scanning of @WebListener is not available for a flat executable JAR, so register it explicitly.
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors during shutdown
      } finally {
        deleteRecursively(workDir);
      }
    }, "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  private static String normalizeContextPath(String path) {
    if ("/".equals(path)) {
      return "";
    }
    return path.startsWith("/") || path.isEmpty() ? path : "/" + path;
  }

  /**
   * Copies the bundled web application (JSPs, WEB-INF, static content) from the JAR (or from target/classes when run
   * from an IDE) into a writable directory used as the Tomcat document base.
   */
  private static void extractWebapp(Path target) throws IOException, URISyntaxException {
    Path source = Paths.get(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (Files.isDirectory(source)) {
      Path root = source.resolve(WEBAPP_RESOURCE_ROOT);
      try (Stream<Path> paths = Files.walk(root)) {
        for (Path path : (Iterable<Path>) paths::iterator) {
          Path destination = target.resolve(root.relativize(path).toString());
          if (Files.isDirectory(path)) {
            Files.createDirectories(destination);
          } else {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
      return;
    }
    String prefix = WEBAPP_RESOURCE_ROOT + "/";
    try (JarFile jar = new JarFile(source.toFile())) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (!entry.getName().startsWith(prefix) || entry.getName().length() == prefix.length()) {
          continue;
        }
        Path destination = target.resolve(entry.getName().substring(prefix.length())).normalize();
        if (!destination.startsWith(target)) {
          continue;
        }
        if (entry.isDirectory()) {
          Files.createDirectories(destination);
        } else {
          Files.createDirectories(destination.getParent());
          try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }
  }

  private static void deleteRecursively(Path dir) {
    try (Stream<Path> paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (IOException | UncheckedIOException e) {
      // best effort cleanup
    }
  }
}
