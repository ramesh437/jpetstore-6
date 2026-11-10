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
import java.net.JarURLConnection;
import java.net.URL;
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
 * Executable entry point that runs JPetStore on an embedded Tomcat (no external application server required).
 * <p>
 * Configuration (environment variables / system properties):
 * </p>
 * <ul>
 * <li>{@code PORT} - HTTP port (default: 8080; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - context path (default: /jpetstore; use an empty value for the root context)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE = "webapp";

  private JpetstoreApplication() {
    // main class only
  }

  /**
   * Starts the embedded Tomcat server.
   *
   * @param args
   *          the command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getSetting("PORT", "8080"));
    String contextPath = getSetting("CONTEXT_PATH", "/jpetstore");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    File docBase = resolveDocBase(baseDir);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector().setProperty("address", getSetting("BIND_ADDRESS", "0.0.0.0"));

    Context context = tomcat.addWebapp(contextPath, docBase.getAbsolutePath());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());
    // @WebListener classes are not discovered by annotation scanning when running from an executable JAR
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        e.printStackTrace();
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static String getSetting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null) {
      value = System.getProperty(name);
    }
    return value == null ? defaultValue : value;
  }

  /**
   * Locates the bundled web content. When running from the executable JAR the content is extracted to a temporary
   * directory; when running from an exploded classpath (IDE, target/classes) it is used in place.
   */
  private static File resolveDocBase(Path baseDir) throws IOException {
    URL url = JpetstoreApplication.class.getClassLoader().getResource(WEBAPP_RESOURCE + "/WEB-INF/web.xml");
    if (url == null) {
      File fallback = new File("src/main/webapp");
      if (fallback.isDirectory()) {
        return fallback;
      }
      throw new IllegalStateException("Bundled web content not found on the classpath: " + WEBAPP_RESOURCE);
    }
    if ("file".equals(url.getProtocol())) {
      // .../webapp/WEB-INF/web.xml -> .../webapp
      return new File(url.getPath()).getParentFile().getParentFile();
    }
    Path target = Files.createDirectories(baseDir.resolve("webapp-content"));
    JarURLConnection connection = (JarURLConnection) url.openConnection();
    try (JarFile jar = connection.getJarFile()) {
      String prefix = WEBAPP_RESOURCE + "/";
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
    return target.toFile();
  }
}
