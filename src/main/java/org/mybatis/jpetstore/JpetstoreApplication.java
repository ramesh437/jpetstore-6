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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application bundled
 * in the JAR (under {@code /webapp}), so that no external application server is required (GKE, Cloud Run, App Engine
 * flexible).
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>PORT - HTTP port to listen on (default: 8080, injected automatically by Cloud Run)</li>
 * <li>CONTEXT_PATH - web application context path (default: empty, i.e. root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_ROOT = "webapp/";

  private JpetstoreApplication() {
    // entry point only
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getEnv("PORT", "8080"));
    String contextPath = getEnv("CONTEXT_PATH", "");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));
    extractWebapp(docBase);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors on shutdown
      } finally {
        deleteQuietly(baseDir);
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void extractWebapp(Path target) throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
        JpetstoreApplication.class.getClassLoader());
    Resource[] resources = resolver.getResources("classpath*:" + WEBAPP_RESOURCE_ROOT + "**");
    for (Resource resource : resources) {
      URL url = resource.getURL();
      String location = url.toString();
      if (location.endsWith("/")) {
        continue;
      }
      int index = location.lastIndexOf("/" + WEBAPP_RESOURCE_ROOT);
      if (index < 0) {
        continue;
      }
      String relative = location.substring(index + WEBAPP_RESOURCE_ROOT.length() + 1);
      Path file = target.resolve(relative).normalize();
      if (!file.startsWith(target)) {
        continue;
      }
      Files.createDirectories(file.getParent());
      try (InputStream in = resource.getInputStream()) {
        Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  private static void deleteQuietly(Path dir) {
    try (Stream<Path> paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (IOException | UncheckedIOException e) {
      // ignore
    }
  }

  private static String getEnv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
