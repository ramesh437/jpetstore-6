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
import java.net.URL;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/**
 * Entry point of the self-contained executable JAR (embedded Tomcat).
 * <p>
 * Replaces the former WAR deployment descriptor ({@code web.xml}): the Spring root context, the Spring MVC dispatcher
 * and the Spring Session filter are all bootstrapped from the XML configuration files on the classpath. The listening
 * port is read from the {@code PORT} environment variable (see {@code application.properties}).
 */
@Configuration(proxyBeanMethods = false)
@Import({ ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class })
@ImportResource({ "classpath:applicationContext.xml", "classpath:spring-mvc.xml" })
public class JpetstoreApplication {

  private static final String WEBAPP_RESOURCE_ROOT = "/webapp";

  public static void main(String[] args) {
    SpringApplication.run(JpetstoreApplication.class, args);
  }

  /**
   * Customizes the embedded Tomcat so that the web content (JSPs, CSS, images) packaged inside the executable JAR
   * under {@code /webapp} is served as the web application root, and the default servlet is available for static
   * content.
   *
   * @return the customizer
   */
  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webappResourcesCustomizer() {
    return factory -> {
      factory.setRegisterDefaultServlet(true);
      factory.addContextCustomizers(JpetstoreApplication::registerWebappResources);
    };
  }

  private static void registerWebappResources(Context context) {
    context.addLifecycleListener(event -> {
      if (!Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
        return;
      }
      URL webapp = JpetstoreApplication.class.getResource(WEBAPP_RESOURCE_ROOT);
      if (webapp == null) {
        return;
      }
      WebResourceRoot resources = context.getResources();
      try {
        String location = webapp.toString();
        if (location.startsWith("jar:")) {
          // e.g. jar:file:/app/jpetstore.jar!/BOOT-INF/classes!/webapp
          int separator = location.indexOf("!/");
          File jar = new File(new URL(location.substring("jar:".length(), separator)).toURI());
          String internalPath = location.substring(separator + 1).replace("!/", "/");
          resources.addPostResources(new JarResourceSet(resources, "/", jar.getAbsolutePath(), internalPath));
        } else {
          File dir = new File(webapp.toURI());
          resources.addPostResources(new DirResourceSet(resources, "/", dir.getAbsolutePath(), "/"));
        }
      } catch (URISyntaxException | java.net.MalformedURLException e) {
        throw new IllegalStateException("Unable to register web resources from " + webapp, e);
      }
    });
  }

}
