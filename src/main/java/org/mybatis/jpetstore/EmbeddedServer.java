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

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Entry point that runs JPetStore as a self-contained executable JAR using an embedded Tomcat, so no external
 * application server is required (suitable for Amazon ECS, EKS and AWS Fargate).
 *
 * <p>
 * Configuration is read from environment variables (or system properties):
 * <ul>
 * <li>PORT (default: 8080) - HTTP port</li>
 * <li>CONTEXT_PATH (default: /jpetstore) - servlet context path, use an empty value for the root context</li>
 * <li>TOMCAT_BASE_DIR (default: a temporary directory) - Tomcat working directory (JSP compilation, etc.)</li>
 * </ul>
 */
public final class EmbeddedServer {

  private static final String WEBAPP_RESOURCE_DIR = "webapp";

  private EmbeddedServer() {
    // utility entry point
  }

  /**
   * Starts the embedded server.
   *
   * @param args
   *          the command line arguments (ignored)
   *
   * @throws Exception
   *           if the server could not be started
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

    String baseDirSetting = getSetting("TOMCAT_BASE_DIR", "");
    File baseDir = baseDirSetting.isEmpty() ? Files.createTempDirectory("jpetstore-tomcat").toFile()
        : new File(baseDirSetting);
    File docBase = new File(baseDir, "webapp");
    if (!docBase.exists() && !docBase.mkdirs()) {
      throw new IllegalStateException("Unable to create " + docBase);
    }

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.getAbsolutePath());
    tomcat.setPort(port);
    tomcat.getConnector();

    Context context = tomcat.addWebapp(contextPath, docBase.getAbsolutePath());
    context.setParentClassLoader(EmbeddedServer.class.getClassLoader());
    // @WebListener classes packaged in the executable JAR are not discovered by Tomcat's WEB-INF/classes scan.
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    WebResourceRoot resources = new StandardRoot(context);
    File location = getCodeSourceLocation();
    if (location.isFile()) {
      resources.createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, "/", location.getAbsolutePath(), null,
          "/" + WEBAPP_RESOURCE_DIR);
    } else {
      resources.createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, "/",
          new File(location, WEBAPP_RESOURCE_DIR).getAbsolutePath(), null, "/");
    }
    context.setResources(resources);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(tomcat), "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      e.printStackTrace();
    }
  }

  private static File getCodeSourceLocation() throws URISyntaxException {
    return new File(EmbeddedServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String getSetting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null) {
      value = System.getProperty(name);
    }
    return value == null ? defaultValue : value;
  }
}
