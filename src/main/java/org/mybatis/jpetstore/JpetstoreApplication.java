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
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Self-contained entry point that runs JPetStore on an embedded Tomcat, so the application is packaged as an
 * executable JAR ({@code java -jar jpetstore.jar}) instead of a WAR deployed to an external application server.
 * <p>
 * Runtime configuration (12-factor, suitable for GKE / Cloud Run / App Engine flexible):
 * <ul>
 * <li>{@code PORT} - HTTP port to listen on (default 8080; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default empty, i.e. served from the root)</li>
 * </ul>
 * The web resources (JSPs, static content and {@code WEB-INF/web.xml}) are served from {@code META-INF/resources}
 * inside the executable JAR.
 */
public final class JpetstoreApplication {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private JpetstoreApplication() {
    // entry point only
  }

  /**
   * Start the embedded server.
   *
   * @param args
   *          command line arguments (ignored)
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

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector().setProperty("address", "0.0.0.0");

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File codeSource = locateCodeSource();
    if (codeSource.isFile()) {
      resources.addPostResources(new JarResourceSet(resources, "/", codeSource.getAbsolutePath(), WEB_RESOURCES_PATH));
    } else {
      resources.addPostResources(
          new DirResourceSet(resources, "/", new File(codeSource, WEB_RESOURCES_PATH).getAbsolutePath(), "/"));
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // nothing more can be done while shutting down
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File locateCodeSource() throws URISyntaxException {
    return new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String getenv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

}
