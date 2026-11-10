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

import org.mybatis.jpetstore.web.configuration.StaxFactoryInitializationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Entry point of the self-contained executable JAR. Starts an embedded Tomcat and bootstraps the existing Spring
 * configuration (previously wired through {@code web.xml} in the WAR deployment).
 * <p>
 * Run with {@code java -jar jpetstore.jar}. The listening port is read from the {@code PORT} environment variable
 * (default {@code 8080}).
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
    RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class, SessionAutoConfiguration.class })
@ImportResource({ "classpath:applicationContext.xml", "classpath:spring-mvc.xml" })
public class JpetstoreApplication {

  /**
   * Application entry point.
   *
   * @param args
   *          the command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(JpetstoreApplication.class, args);
  }

  /**
   * Registers the servlet context listener that was previously declared through {@code @WebListener}.
   *
   * @return the listener registration
   */
  @Bean
  public ServletListenerRegistrationBean<StaxFactoryInitializationListener> staxFactoryInitializationListener() {
    return new ServletListenerRegistrationBean<>(new StaxFactoryInitializationListener());
  }
}
