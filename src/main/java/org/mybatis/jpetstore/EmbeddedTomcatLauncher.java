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
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application, so no
 * external application server (WAR deployment) is required. Suitable for GKE, Cloud Run and App Engine flexible.
 *
 * <p>
 * Configuration is read from environment variables:
 * <ul>
 * <li>{@code PORT} (or {@code SERVER_PORT}) - HTTP port, defaults to 8080 (Cloud Run injects {@code PORT})</li>
 * <li>{@code CONTEXT_PATH} - servlet context path, defaults to {@code /jpetstore}; use an empty value for root</li>
 * </ul>
 */
public final class EmbeddedTomcatLauncher {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private EmbeddedTomcatLauncher() {
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(firstNonBlank(System.getenv("PORT"), System.getenv("SERVER_PORT"), "8080"));
    String contextPath = System.getenv("CONTEXT_PATH") == null ? "/jpetstore" : System.getenv("CONTEXT_PATH").trim();
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    File docBase = resolveWebappDirectory(baseDir);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());

    Connector connector = new Connector("HTTP/1.1");
    connector.setPort(port);
    tomcat.getService().addConnector(connector);
    tomcat.setConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.getAbsolutePath());
    context.setParentClassLoader(EmbeddedTomcatLauncher.class.getClassLoader());

    tomcat.start();
    tomcat.getServer().await();
  }

  /**
   * Locates the web content: a directory on the classpath when running from the IDE/build output, or the
   * {@code webapp/} folder extracted from the executable JAR.
   */
  private static File resolveWebappDirectory(Path baseDir) throws IOException, URISyntaxException {
    CodeSource codeSource = EmbeddedTomcatLauncher.class.getProtectionDomain().getCodeSource();
    File location = new File(codeSource.getLocation().toURI());
    if (location.isDirectory()) {
      File dir = new File(location, WEBAPP_RESOURCE_DIR);
      if (dir.isDirectory()) {
        return dir;
      }
      File sourceDir = new File("src/main/webapp");
      if (sourceDir.isDirectory()) {
        return sourceDir;
      }
      throw new IllegalStateException("Web content not found in " + location);
    }

    Path target = Files.createDirectories(baseDir.resolve("webapps-content"));
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
    return target.toFile();
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        return value.trim();
      }
    }
    return null;
  }
}
