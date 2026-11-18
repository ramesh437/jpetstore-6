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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis (via Spring Session), so that the application
 * instances remain stateless and can scale horizontally.
 * <p>
 * Configuration is read from environment variables (or JVM system properties of the same name):
 * <ul>
 * <li>REDIS_HOST (default: localhost) - Memorystore instance IP / host</li>
 * <li>REDIS_PORT (default: 6379)</li>
 * <li>REDIS_PASSWORD (optional) - Memorystore AUTH string</li>
 * <li>REDIS_SSL (default: false) - enable for Memorystore in-transit encryption (TLS)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Lettuce connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(setting("REDIS_HOST", "localhost"),
        Integer.parseInt(setting("REDIS_PORT", "6379")));
    String password = setting("REDIS_PASSWORD", null);
    if (password != null) {
      redisConfig.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(setting("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(redisConfig, builder.build());
  }

  /**
   * Memorystore is a managed service and does not allow the CONFIG command, so disable Spring Session's automatic
   * keyspace-notification configuration.
   *
   * @return a no-op configure action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
