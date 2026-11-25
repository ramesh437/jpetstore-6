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
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Executable entry point that runs JPetStore on an embedded Tomcat so the application can be packaged as a
 * self-contained JAR (GKE / Cloud Run / App Engine flexible).
 *
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port (default {@code 8080}; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default empty, i.e. root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE = "webapp";
  private static final String DEFAULT_PORT = "8080";

  private JpetstoreApplication() {
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getSetting("PORT", DEFAULT_PORT));
    String contextPath = getSetting("CONTEXT_PATH", "");
    Tomcat tomcat = start(port, contextPath);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "tomcat-shutdown"));
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
   * @throws Exception
   *           if the server cannot be started
   */
  public static Tomcat start(int port, String contextPath) throws Exception {
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapps").resolve("ROOT"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(JpetstoreApplication.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    resources.addPreResources(createWebappResourceSet(resources));
    context.setResources(resources);

    tomcat.start();
    return tomcat;
  }

  private static org.apache.catalina.WebResourceSet createWebappResourceSet(WebResourceRoot root)
      throws IOException, URISyntaxException {
    URL webXml = JpetstoreApplication.class.getClassLoader().getResource(WEBAPP_RESOURCE + "/WEB-INF/web.xml");
    if (webXml == null) {
      throw new IllegalStateException("Web application resources not found on classpath: /" + WEBAPP_RESOURCE);
    }
    if ("jar".equals(webXml.getProtocol())) {
      URLConnection connection = webXml.openConnection();
      URL jarUrl = ((JarURLConnection) connection).getJarFileURL();
      String jarPath = new File(jarUrl.toURI()).getAbsolutePath();
      return new JarResourceSet(root, "/", jarPath, "/" + WEBAPP_RESOURCE);
    }
    // Running from an exploded classpath (IDE / tests)
    Path webInf = Paths.get(webXml.toURI()).getParent();
    return new DirResourceSet(root, "/", webInf.getParent().toAbsolutePath().toString(), "/");
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      // best effort on shutdown
      System.err.println("Error while stopping embedded Tomcat: " + e.getMessage());
    }
  }

  private static String getSetting(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

}
