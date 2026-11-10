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
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state (account, cart, order) into Google Cloud Memorystore for Redis so that the
 * application instances stay stateless and can scale horizontally.
 * <p>
 * Connection settings are read from environment variables:
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore host/IP (default: localhost)</li>
 * <li>{@code REDIS_PORT} - Memorystore port (default: 6379)</li>
 * <li>{@code REDIS_AUTH} - optional AUTH string (default: none)</li>
 * <li>{@code SESSION_TIMEOUT_SECONDS} - session TTL in seconds (default: 1800)</li>
 * <li>{@code SESSION_REDIS_NAMESPACE} - key namespace (default: jpetstore:session)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  /**
   * Redis connection factory pointing to Memorystore for Redis.
   *
   * @return the lettuce connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(getEnv("REDIS_HOST", "localhost"),
        Integer.parseInt(getEnv("REDIS_PORT", "6379")));
    String auth = System.getenv("REDIS_AUTH");
    if (auth != null && !auth.isEmpty()) {
      config.setPassword(RedisPassword.of(auth));
    }
    return new LettuceConnectionFactory(config);
  }

  private static String getEnv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }
}
