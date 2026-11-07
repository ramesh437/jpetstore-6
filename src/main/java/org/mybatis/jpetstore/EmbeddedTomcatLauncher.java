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
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat server and deploys the web application
 * (bundled under <code>/webapp</code> in the JAR) so that the application can run on GKE, Cloud Run or App Engine
 * flexible without an external application server.
 *
 * <p>
 * Configuration (12-factor, environment variables or JVM system properties with the same name):
 * <ul>
 * <li>PORT (default: 8080) - HTTP port, injected by Cloud Run / App Engine</li>
 * <li>CONTEXT_PATH (default: empty = root context)</li>
 * </ul>
 */
public final class EmbeddedTomcatLauncher {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private EmbeddedTomcatLauncher() {
  }

  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(setting("PORT", "8080"));
    String contextPath = setting("CONTEXT_PATH", "");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path webappDir = baseDir.resolve(WEBAPP_RESOURCE_DIR);
    Files.createDirectories(webappDir);
    extractWebapp(webappDir);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, webappDir.toAbsolutePath().toString());
    // Annotation scanning of classpath classes is not performed for an executable JAR, register explicitly.
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore on shutdown
      } finally {
        deleteQuietly(baseDir);
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void extractWebapp(Path target) throws IOException, URISyntaxException {
    Path source = Paths.get(EmbeddedTomcatLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (Files.isDirectory(source)) {
      Path webappSource = source.resolve(WEBAPP_RESOURCE_DIR);
      try (Stream<Path> paths = Files.walk(webappSource)) {
        paths.forEach(p -> {
          Path dest = target.resolve(webappSource.relativize(p).toString());
          try {
            if (Files.isDirectory(p)) {
              Files.createDirectories(dest);
            } else {
              Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
            }
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      }
      return;
    }
    String prefix = WEBAPP_RESOURCE_DIR + "/";
    try (JarFile jar = new JarFile(source.toFile())) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (!entry.getName().startsWith(prefix) || entry.getName().length() == prefix.length()) {
          continue;
        }
        Path dest = target.resolve(entry.getName().substring(prefix.length())).normalize();
        if (!dest.startsWith(target)) {
          continue;
        }
        if (entry.isDirectory()) {
          Files.createDirectories(dest);
        } else {
          Files.createDirectories(dest.getParent());
          try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }
  }

  private static void deleteQuietly(Path dir) {
    try (Stream<Path> paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    } catch (IOException e) {
      // ignore
    }
  }
}
