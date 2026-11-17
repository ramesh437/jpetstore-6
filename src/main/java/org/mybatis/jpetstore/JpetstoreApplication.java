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
import java.net.JarURLConnection;
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
import org.apache.catalina.startup.Tomcat;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application, so the
 * application no longer needs an external application server (GKE, Cloud Run, App Engine flexible).
 *
 * <p>
 * Runtime settings (12-factor): {@code PORT} (default 8080, set automatically by Cloud Run) and {@code CONTEXT_PATH}
 * (default: root context).
 * </p>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";
  private static final String WEB_XML_RESOURCE = WEBAPP_RESOURCE_DIR + "/WEB-INF/web.xml";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Start the embedded server.
   *
   * @param args
   *          the command line arguments (not used)
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(setting("PORT", "8080"));
    String contextPath = setting("CONTEXT_PATH", "");

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = resolveDocBase(baseDir);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());
    // Classpath scanning for @WebListener is not performed for classes of the executable JAR; register explicitly.
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      System.err.println("Failed to stop embedded Tomcat gracefully: " + e.getMessage());
    }
  }

  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Locate the web application content packaged on the classpath. When running from the executable JAR the content is
   * extracted to a temporary directory (required by the JSP compiler); when running from build output it is used in
   * place.
   */
  private static Path resolveDocBase(Path baseDir) throws IOException, URISyntaxException {
    URL webXml = JpetstoreApplication.class.getClassLoader().getResource(WEB_XML_RESOURCE);
    if (webXml == null) {
      throw new IllegalStateException("Web application resources not found on classpath: " + WEB_XML_RESOURCE);
    }
    if ("file".equals(webXml.getProtocol())) {
      return Paths.get(webXml.toURI()).getParent().getParent();
    }
    Path target = baseDir.resolve(WEBAPP_RESOURCE_DIR);
    Files.createDirectories(target);
    JarURLConnection connection = (JarURLConnection) webXml.openConnection();
    connection.setUseCaches(false);
    try (JarFile jar = connection.getJarFile()) {
      String prefix = WEBAPP_RESOURCE_DIR + "/";
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (!entry.getName().startsWith(prefix) || entry.getName().length() == prefix.length()) {
          continue;
        }
        Path out = baseDir.resolve(entry.getName()).normalize();
        if (!out.startsWith(target)) {
          throw new UncheckedIOException(new IOException("Illegal entry name: " + entry.getName()));
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
