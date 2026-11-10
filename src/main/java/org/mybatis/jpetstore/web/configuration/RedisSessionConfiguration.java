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
package org.mybatis.jpetstore.web.configuration;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis so that application instances stay stateless.
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore host/IP (default {@code localhost})</li>
 * <li>{@code REDIS_PORT} - Memorystore port (default {@code 6379})</li>
 * <li>{@code REDIS_PASSWORD} - AUTH string, optional (inject from Secret Manager)</li>
 * <li>{@code REDIS_SSL} - {@code true} to enable in-transit encryption (default {@code false})</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfiguration {

  /**
   * Lettuce connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    String host = env("REDIS_HOST", "localhost");
    int port = Integer.parseInt(env("REDIS_PORT", "6379"));
    String password = System.getenv("REDIS_PASSWORD");

    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
    if (password != null && !password.isEmpty()) {
      serverConfig.setPassword(RedisPassword.of(password));
    }

    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.ofSeconds(5));
    if (Boolean.parseBoolean(env("REDIS_SSL", "false"))) {
      clientConfig.useSsl();
    }
    return new LettuceConnectionFactory(serverConfig, clientConfig.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable Spring Session's keyspace notification auto-config.
   *
   * @return a no-op redis action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
