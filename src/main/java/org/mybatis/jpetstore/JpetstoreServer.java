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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web
 * application, so no external application server (WAR deployment) is required.
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} (Cloud Run / GKE convention) or {@code SERVER_PORT} - HTTP port, defaults to 8080</li>
 * <li>{@code SERVER_CONTEXT_PATH} - context path, defaults to {@code /jpetstore}</li>
 * </ul>
 */
public final class JpetstoreServer {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private static final int DEFAULT_PORT = 8080;

  private static final String DEFAULT_CONTEXT_PATH = "/jpetstore";

  private Tomcat tomcat;

  private Path workDir;

  /**
   * Starts the embedded server using the given port and context path.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for root)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public void start(int port, String contextPath) throws Exception {
    workDir = Files.createTempDirectory("jpetstore-");
    Path docBase = prepareWebappDirectory(workDir);

    tomcat = new Tomcat();
    tomcat.setBaseDir(workDir.resolve("tomcat").toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toAbsolutePath().toString());
    context.setParentClassLoader(Thread.currentThread().getContextClassLoader());
    // @WebListener classes live on the application classpath, not in WEB-INF/classes, so register explicitly.
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    tomcat.start();
  }

  /**
   * Stops the embedded server and removes temporary files.
   *
   * @throws LifecycleException
   *           if the server cannot be stopped
   */
  public void stop() throws LifecycleException {
    if (tomcat != null) {
      tomcat.stop();
      tomcat.destroy();
      tomcat = null;
    }
    if (workDir != null) {
      deleteQuietly(workDir.toFile());
      workDir = null;
    }
  }

  /**
   * Blocks the calling thread until the server is stopped.
   */
  public void await() {
    if (tomcat != null) {
      tomcat.getServer().await();
    }
  }

  public static void main(String[] args) throws Exception {
    int port = resolvePort();
    String contextPath = resolveContextPath();

    JpetstoreServer server = new JpetstoreServer();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        server.stop();
      } catch (LifecycleException e) {
        e.printStackTrace();
      }
    }, "jpetstore-shutdown"));

    server.start(port, contextPath);
    server.await();
  }

  private static int resolvePort() {
    String value = System.getenv("PORT");
    if (value == null || value.isBlank()) {
      value = System.getenv("SERVER_PORT");
    }
    if (value == null || value.isBlank()) {
      return DEFAULT_PORT;
    }
    return Integer.parseInt(value.trim());
  }

  private static String resolveContextPath() {
    String value = System.getenv("SERVER_CONTEXT_PATH");
    if (value == null) {
      return DEFAULT_CONTEXT_PATH;
    }
    value = value.trim();
    if (value.isEmpty() || "/".equals(value)) {
      return "";
    }
    return value.startsWith("/") ? value : "/" + value;
  }

  /**
   * Makes the bundled web application content available on the file system (JSP compilation requires it).
   */
  private static Path prepareWebappDirectory(Path baseDir) throws IOException, URISyntaxException {
    File location = new File(JpetstoreServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isDirectory()) {
      // Running from exploded classes (e.g. IDE or integration tests): webapp content was copied to <classes>/webapp
      return new File(location, WEBAPP_RESOURCE_DIR).toPath();
    }
    Path target = Files.createDirectories(baseDir.resolve(WEBAPP_RESOURCE_DIR));
    String prefix = WEBAPP_RESOURCE_DIR + "/";
    try (JarFile jar = new JarFile(location)) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (!entry.getName().startsWith(prefix) || entry.getName().length() == prefix.length()) {
          continue;
        }
        Path out = target.resolve(entry.getName().substring(prefix.length())).normalize();
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

  private static void deleteQuietly(File file) {
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) {
        deleteQuietly(child);
      }
    }
    file.delete();
  }

}
