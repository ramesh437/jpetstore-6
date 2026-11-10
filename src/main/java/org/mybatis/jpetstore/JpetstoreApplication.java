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
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;
import org.mybatis.jpetstore.web.session.SessionIdFilter;

/**
 * Entry point of the self-contained (executable JAR) JPetStore application. It starts an embedded Apache Tomcat that
 * serves the web application resources packaged under {@code META-INF/resources} (including
 * {@code WEB-INF/web.xml}, the Spring configuration and the JSP views), so that no external application server or WAR
 * deployment is required (GKE, Cloud Run, App Engine flexible).
 *
 * <p>
 * Runtime configuration (12-factor), all optional:
 * <ul>
 * <li>{@code PORT} - HTTP port to listen on (default {@code 8080}; Cloud Run injects this variable)</li>
 * <li>{@code CONTEXT_PATH} - context path of the application (default: root context)</li>
 * <li>{@code TOMCAT_BASE_DIR} - writable scratch directory for Tomcat/JSP work files (default: a temp directory)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private JpetstoreApplication() {
    // main class only
  }

  /**
   * Starts the embedded server.
   *
   * @param args
   *          command line arguments (not used)
   *
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = normalizeContextPath(env("CONTEXT_PATH", ""));

    Path baseDir = resolveBaseDir();
    Path docBase = Files.createDirectories(baseDir.resolve("docbase"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    tomcat.setPort(port);
    tomcat.getConnector();
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addContext(contextPath, docBase.toString());
    context.setResources(createWebResources(context));
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    FilterDef filterDef = new FilterDef();
    filterDef.setFilterName("sessionIdFilter");
    filterDef.setFilterClass(SessionIdFilter.class.getName());
    context.addFilterDef(filterDef);
    FilterMap filterMap = new FilterMap();
    filterMap.setFilterName("sessionIdFilter");
    filterMap.addURLPattern("/*");
    context.addFilterMap(filterMap);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        // shutting down anyway
      }
    }, "jpetstore-shutdown"));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static WebResourceRoot createWebResources(Context context) throws URISyntaxException {
    StandardRoot root = new StandardRoot(context);
    File location = new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    if (location.isFile()) {
      root.addPostResources(new JarResourceSet(root, "/", location.getAbsolutePath(), WEB_RESOURCES_PATH));
    } else {
      root.addPostResources(new DirResourceSet(root, "/", location.getAbsolutePath(), WEB_RESOURCES_PATH));
    }
    return root;
  }

  private static Path resolveBaseDir() throws java.io.IOException {
    String configured = env("TOMCAT_BASE_DIR", "");
    if (!configured.isEmpty()) {
      return Files.createDirectories(new File(configured).toPath());
    }
    return Files.createTempDirectory("jpetstore-tomcat");
  }

  private static String normalizeContextPath(String value) {
    String path = value.trim();
    if (path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
