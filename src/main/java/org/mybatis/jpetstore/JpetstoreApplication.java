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
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;

/**
 * Executable entry point that runs JPetStore on an embedded Tomcat (GKE / Cloud Run / App Engine flexible).
 * <p>
 * Configuration (12-factor, via environment variables):
 * <ul>
 * <li>{@code PORT} - HTTP port (default 8080, injected by Cloud Run)</li>
 * <li>{@code CONTEXT_PATH} - servlet context path (default {@code /jpetstore}; use empty/{@code /} for root)</li>
 * </ul>
 */
public final class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_PATH = "/META-INF/webapp";

  private JpetstoreApplication() {
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(getEnv("PORT", "8080"));
    String contextPath = normalizeContextPath(getEnv("CONTEXT_PATH", "/jpetstore"));

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(Files.createTempDirectory("jpetstore-tomcat").toAbsolutePath().toString());

    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getService().addConnector(connector);
    tomcat.getHost().setAutoDeploy(false);

    File docBase = Files.createTempDirectory("jpetstore-webapp").toFile();
    Context context = tomcat.addWebapp(contextPath, docBase.getAbsolutePath());

    WebResourceRoot resources = new StandardRoot(context);
    File location = getCodeSourceLocation();
    WebResourceSet webappResources;
    if (location.isDirectory()) {
      webappResources = new DirResourceSet(resources, "/", new File(location, WEBAPP_RESOURCE_PATH).getAbsolutePath(),
          "/");
    } else {
      webappResources = new JarResourceSet(resources, "/", location.getAbsolutePath(), WEBAPP_RESOURCE_PATH);
    }
    resources.addPreResources(webappResources);
    context.setResources(resources);

    // @WebListener classes are not scanned when running embedded, register explicitly
    context.addApplicationListener(StaxFactoryInitializationListener.class.getName());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        // ignore on shutdown
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static File getCodeSourceLocation() throws URISyntaxException {
    return new File(JpetstoreApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static String getEnv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  private static String normalizeContextPath(String path) {
    if (path.isEmpty() || "/".equals(path)) {
      return "";
    }
    return path.startsWith("/") ? path : "/" + path;
  }
}
