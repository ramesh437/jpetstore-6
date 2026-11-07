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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Stores HTTP session state in Google Cloud Memorystore for Redis so the application instances stay stateless.
 * <p>
 * Configuration (environment variables or system properties):
 * <ul>
 * <li>REDIS_HOST (default: localhost)</li>
 * <li>REDIS_PORT (default: 6379)</li>
 * <li>REDIS_PASSWORD (optional, Memorystore AUTH string)</li>
 * <li>REDIS_SSL (optional, "true" to enable in-transit encryption)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Redis connection factory pointing at Memorystore.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(setting("REDIS_HOST", "localhost"),
        Integer.parseInt(setting("REDIS_PORT", "6379")));
    String password = setting("REDIS_PASSWORD", null);
    if (password != null) {
      config.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(setting("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable automatic keyspace notification configuration.
   *
   * @return the no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
