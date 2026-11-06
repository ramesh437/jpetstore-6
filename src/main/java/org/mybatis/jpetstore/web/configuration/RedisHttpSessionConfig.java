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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes the HTTP session state to Google Cloud Memorystore for Redis (Spring Session), so that the application
 * instances are stateless and can be scaled horizontally.
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore host/IP (default: localhost)</li>
 * <li>{@code REDIS_PORT} - Memorystore port (default: 6379)</li>
 * <li>{@code REDIS_AUTH} - Memorystore AUTH string (optional)</li>
 * <li>{@code REDIS_SSL} - set to {@code true} to enable in-transit encryption (default: false)</li>
 * <li>{@code SESSION_TIMEOUT_SECONDS} - session TTL in seconds (default: 1800)</li>
 * <li>{@code SESSION_NAMESPACE} - Redis key namespace (default: jpetstore:session)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession
public class RedisHttpSessionConfig {

  /**
   * Redis connection factory pointing to Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(env("REDIS_HOST", "localhost"),
        Integer.parseInt(env("REDIS_PORT", "6379")));
    String auth = System.getenv("REDIS_AUTH");
    if (auth != null && !auth.isEmpty()) {
      configuration.setPassword(RedisPassword.of(auth));
    }
    LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
    factory.setUseSsl(Boolean.parseBoolean(env("REDIS_SSL", "false")));
    return factory;
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable Spring Session's keyspace notification auto-config.
   *
   * @return the no-op action
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
