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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the JPetStore web
 * application (previously deployed as a WAR on an external application server).
 *
 * <p>
 * Runtime configuration (12-factor):
 * <ul>
 * <li>{@code PORT} - HTTP port (default 8080; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default: root context)</li>
 * <li>{@code TOMCAT_BASE_DIR} - writable work directory (default: a temp directory)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_PATH = "/webapp";

  private JpetstoreApplication() {
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
    String contextPath = getenv("CONTEXT_PATH", "");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    String baseDirName = System.getenv("TOMCAT_BASE_DIR");
    Path baseDir = baseDirName == null || baseDirName.isEmpty() ? Files.createTempDirectory("jpetstore-tomcat")
        : Files.createDirectories(new File(baseDirName).toPath());
    Path docBase = Files.createDirectories(baseDir.resolve("webapps-root"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toAbsolutePath().toString());
    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    StandardContext context = (StandardContext) tomcat.addWebapp(contextPath, docBase.toAbsolutePath().toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());
    configureWebResources(context);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // ignore errors during shutdown
      }
    }, "tomcat-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void configureWebResources(Context context) throws URISyntaxException {
    WebResourceRoot resources = new StandardRoot(context);
    File codeSource = new File(
        JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (codeSource.isFile()) {
      // Running from the executable JAR: web content lives under /webapp inside the archive
      resources.addPreResources(
          new JarResourceSet(resources, "/", codeSource.getAbsolutePath(), WEBAPP_RESOURCE_PATH));
    } else {
      // Running from an exploded classes directory (IDE / target/classes)
      resources.addPreResources(
          new DirResourceSet(resources, "/", new File(codeSource, "webapp").getAbsolutePath(), "/"));
    }
    context.setResources(resources);
  }

  private static String getenv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
