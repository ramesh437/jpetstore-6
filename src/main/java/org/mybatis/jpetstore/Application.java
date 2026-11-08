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
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Executable entry point that runs JPetStore on an embedded Tomcat (replaces WAR deployment to an external
 * application server). Suitable for GKE, Cloud Run or App Engine flexible: {@code java -jar jpetstore.jar}.
 *
 * <p>
 * Runtime configuration (12-factor):
 * <ul>
 * <li>{@code PORT} / {@code SERVER_PORT} - HTTP port (default 8080; Cloud Run injects {@code PORT})</li>
 * <li>{@code SERVER_ADDRESS} - bind address (default 0.0.0.0)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default /jpetstore)</li>
 * </ul>
 */
public final class Application {

  private static final String WEB_RESOURCES_PATH = "/META-INF/resources";

  private Application() {
  }

  public static void main(String[] args) throws Exception {
    String portValue = firstNonEmpty(System.getenv("PORT"), System.getenv("SERVER_PORT"), "8080");
    String address = firstNonEmpty(System.getenv("SERVER_ADDRESS"), "0.0.0.0");
    String contextPath = normalizeContextPath(firstNonEmpty(System.getenv("CONTEXT_PATH"), "/jpetstore"));

    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());

    Connector connector = tomcat.getConnector();
    connector.setPort(Integer.parseInt(portValue));
    connector.setProperty("address", address);

    Context context = tomcat.addWebapp(contextPath, docBase.toString());
    context.setParentClassLoader(Application.class.getClassLoader());
    // Registered programmatically because classpath annotation scanning is not available in an executable JAR.
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    // Serve JSPs, WEB-INF configuration and static content packaged on the classpath under META-INF/resources.
    WebResourceRoot resources = new StandardRoot(context);
    // Works both for the executable JAR (JarResourceSet) and for a classes directory (DirResourceSet).
    resources.createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, "/", codeSource().getAbsolutePath(), null,
        WEB_RESOURCES_PATH);
    context.setResources(resources);

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File codeSource() throws URISyntaxException {
    return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String normalizeContextPath(String path) {
    if (path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }

  private static String firstNonEmpty(String... values) {
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        return value.trim();
      }
    }
    return "";
  }
}
