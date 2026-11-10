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
package org.mybatis.jpetstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis (Spring Session), so that the application
 * instances stay stateless and can be scaled horizontally.
 *
 * <p>
 * Configuration is read from environment variables (or JVM system properties with the same name):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance host / IP (default {@code localhost})</li>
 * <li>{@code REDIS_PORT} - Memorystore instance port (default {@code 6379})</li>
 * <li>{@code REDIS_AUTH} - Memorystore AUTH string (optional)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  /**
   * Redis connection factory pointing to Memorystore for Redis.
   *
   * @return the redis connection factory
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(getSetting("REDIS_HOST", "localhost"),
        Integer.parseInt(getSetting("REDIS_PORT", "6379")));
    String auth = getSetting("REDIS_AUTH", null);
    if (auth != null && !auth.isEmpty()) {
      configuration.setPassword(auth);
    }
    return new LettuceConnectionFactory(configuration);
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable automatic keyspace notification configuration.
   *
   * @return the configure redis action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }

  private static String getSetting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
