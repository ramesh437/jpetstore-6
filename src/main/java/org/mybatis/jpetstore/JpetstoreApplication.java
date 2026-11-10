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

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

/**
 * Entry point of the self-contained executable JAR (embedded Tomcat). Replaces the former WEB-INF/web.xml +
 * external servlet container deployment of the WAR. The data source, MyBatis, transaction and Spring Session
 * (Memorystore for Redis) wiring remains in {@code applicationContext.xml}.
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class, RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class,
    SessionAutoConfiguration.class })
@ImportResource("classpath:applicationContext.xml")
@ServletComponentScan(basePackages = "org.mybatis.jpetstore.web.configuration")
public class JpetstoreApplication {

  /**
   * Starts the application.
   *
   * @param args
   *          the command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(JpetstoreApplication.class, args);
  }

  /**
   * Registers the Spring Session repository filter (equivalent of the former web.xml DelegatingFilterProxy mapping)
   * as the first filter of the chain.
   *
   * @param springSessionRepositoryFilter
   *          the session filter defined by the Spring Session configuration
   *
   * @return the filter registration
   */
  @Bean
  public FilterRegistrationBean<Filter> sessionRepositoryFilterRegistration(
      @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter) {
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(springSessionRepositoryFilter);
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC));
    registration.setOrder(Integer.MIN_VALUE + 50);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
