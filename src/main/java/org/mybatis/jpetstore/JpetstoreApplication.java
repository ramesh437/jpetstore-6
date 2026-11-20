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
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat that serves the web application
 * resources packaged under {@code META-INF/resources} (including {@code WEB-INF/web.xml}), so no external application
 * server is required.
 * <p>
 * Configuration (12-factor, via environment variables): {@code PORT} (default 8080, as injected by Cloud Run) and
 * {@code CONTEXT_PATH} (default {@code /jpetstore}, use an empty value to serve from the root).
 */
public final class JpetstoreApplication {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

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
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = env("CONTEXT_PATH", "/jpetstore");
    if ("/".equals(contextPath)) {
      contextPath = "";
    }

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getHost().setAppBase(baseDir.toString());

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File location = codeSource();
    WebResourceSet webResources;
    if (location.isDirectory()) {
      webResources = new DirResourceSet(resources, "/", new File(location, WEB_RESOURCES_PATH).getAbsolutePath(), "/");
    } else {
      webResources = new JarResourceSet(resources, "/", location.getAbsolutePath(), WEB_RESOURCES_PATH);
    }
    resources.addPreResources(webResources);
    ((StandardContext) context).setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore errors on shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File codeSource() throws URISyntaxException {
    return new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null ? defaultValue : value.trim();
  }
}
