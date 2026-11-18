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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

/**
 * Executable entry point that runs JPetStore on an embedded Tomcat, so the application can be shipped as a
 * self-contained JAR (GKE / Cloud Run / App Engine flexible) instead of a WAR deployed to an external server.
 *
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>PORT (default: 8080) - HTTP port (Cloud Run injects this)</li>
 * <li>CONTEXT_PATH (default: empty / root) - servlet context path</li>
 * </ul>
 */
public final class JPetStoreApplication {

  private static final String WEB_XML_RESOURCE = "META-INF/resources/WEB-INF/web.xml";

  private JPetStoreApplication() {
  }

  public static void main(String[] args) throws LifecycleException, IOException {
    int port = Integer.parseInt(env("PORT", "8080"));
    String contextPath = env("CONTEXT_PATH", "");
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }

    // Writable base dir for Tomcat work files and the web.xml descriptor (container filesystem may be read-only
    // except for the temp dir).
    Path baseDir = Files.createTempDirectory("jpetstore-tomcat");
    Path docBase = Files.createDirectories(baseDir.resolve("webapp"));
    Path webInf = Files.createDirectories(docBase.resolve("WEB-INF"));
    try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(WEB_XML_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Cannot find " + WEB_XML_RESOURCE + " on the classpath");
      }
      Files.copy(in, webInf.resolve("web.xml"), StandardCopyOption.REPLACE_EXISTING);
    }

    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toFile().getAbsolutePath());

    Connector connector = new Connector();
    connector.setPort(port);
    tomcat.setConnector(connector);
    tomcat.getService().addConnector(connector);
    tomcat.setHostname("0.0.0.0");

    // Static resources, JSPs and the remaining WEB-INF content are served from META-INF/resources in this JAR.
    Context context = tomcat.addWebapp(contextPath, new File(docBase.toString()).getAbsolutePath());
    context.setParentClassLoader(JPetStoreApplication.class.getClassLoader());

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (LifecycleException e) {
        e.printStackTrace();
      }
    }));

    tomcat.start();
    tomcat.getServer().await();
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
