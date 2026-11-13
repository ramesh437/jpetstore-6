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
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

/**
 * Entry point that runs JPetStore as a self-contained executable JAR using an embedded Tomcat server.
 * <p>
 * Configuration is read from environment variables (12-factor): {@code PORT} (default 8080), {@code CONTEXT_PATH}
 * (default /jpetstore, because seed data references /jpetstore/images) and {@code BIND_ADDRESS} (default 0.0.0.0). The {@code PORT} variable is injected by Cloud
 * Run.
 */
public final class Application {

  private Application() {
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Starts the embedded server using environment based configuration and blocks until it is stopped.
   *
   * @param args
   *          the command line arguments (not used)
   * @throws Exception
   *           if the server cannot be started
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(env("PORT", "8080"));
    Tomcat tomcat = start(port, env("CONTEXT_PATH", "/jpetstore"));
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
   * @return the started server
   * @throws Exception
   *           if the server cannot be started
   */
  public static Tomcat start(int port, String contextPath) throws Exception {
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Connector connector = new Connector("HTTP/1.1");
    connector.setPort(port);
    tomcat.getService().addConnector(connector);
    tomcat.setConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(Application.class.getClassLoader());

    WebResourceRoot resources = new StandardRoot(context);
    File location = codeLocation();
    if (location.isFile()) {
      // executable JAR: web resources are packaged under /webapp inside the JAR
      resources.addPreResources(new JarResourceSet(resources, "/", location.getAbsolutePath(), "/webapp"));
    } else {
      // exploded classes directory (IDE / tests)
      File webapp = new File(location, "webapp");
      if (!webapp.isDirectory()) {
        webapp = new File("src/main/webapp");
      }
      resources.addPreResources(new DirResourceSet(resources, "/", webapp.getAbsolutePath(), "/"));
    }
    context.setResources(resources);

    // Listener was discovered through @WebListener scanning in a WAR; register explicitly for the executable JAR.
    context.addApplicationListener("org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener");

    // JSTL TLDs are located on the classpath
    StandardJarScanner scanner = new StandardJarScanner();
    StandardJarScanFilter filter = new StandardJarScanFilter();
    filter.setTldSkip("");
    filter.setTldScan("*");
    scanner.setJarScanFilter(filter);
    context.setJarScanner(scanner);

    try {
      tomcat.start();
    } catch (LifecycleException e) {
      throw new IllegalStateException("Unable to start embedded Tomcat", e);
    }
    return tomcat;
  }

  /**
   * Stops and destroys the given server.
   *
   * @param tomcat
   *          the server to stop
   */
  public static void stop(Tomcat tomcat) {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      // ignore errors on shutdown
    }
  }

  private static File codeLocation() throws URISyntaxException {
    return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }
}
