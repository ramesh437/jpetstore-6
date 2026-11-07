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
 * Executable entry point that runs JPetStore on an embedded Tomcat, so the application is delivered as a
 * self-contained JAR (no external application server / WAR deployment needed) and can run on GKE, Cloud Run or App
 * Engine flexible.
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li><code>PORT</code> - HTTP port to listen on (default: 8080, the Cloud Run convention)</li>
 * <li><code>CONTEXT_PATH</code> - servlet context path (default: empty / root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Starts the embedded server and blocks until it is stopped.
   *
   * @param args
   *          the command line arguments (unused)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = env("CONTEXT_PATH", "");
    Tomcat tomcat = start(port, contextPath);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "jpetstore-shutdown"));
    tomcat.getServer().await();
  }

  /**
   * Starts an embedded Tomcat serving the JPetStore web application.
   *
   * @param port
   *          the HTTP port
   * @param contextPath
   *          the context path ("" for root)
   *
   * @return the started Tomcat instance
   *
   * @throws IOException
   *           if the web application resources cannot be prepared
   * @throws LifecycleException
   *           if Tomcat fails to start
   */
  public static Tomcat start(int port, String contextPath) throws IOException, LifecycleException {
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    String docBase = resolveDocBase(baseDir);

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    Context context = tomcat.addWebapp(contextPath, docBase);
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());
    tomcat.start();
    return tomcat;
  }

  /**
   * Stops the given embedded Tomcat instance.
   *
   * @param tomcat
   *          the Tomcat instance
   */
  public static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      throw new IllegalStateException("Failed to stop embedded Tomcat", e);
    }
  }

  private static String resolveDocBase(Path baseDir) throws IOException {
    File location;
    try {
      location = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException e) {
      throw new IOException("Cannot locate application resources", e);
    }
    if (location.isDirectory()) {
      // running from an exploded classpath (IDE, target/classes)
      return new File(location, WEBAPP_RESOURCE_DIR).getAbsolutePath();
    }
    // running from the executable JAR: extract the bundled web resources to an ephemeral directory
    Path target = baseDir.resolve(WEBAPP_RESOURCE_DIR);
    Files.createDirectories(target);
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
          throw new IOException("Illegal entry in archive: " + entry.getName());
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
    return target.toString();
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
