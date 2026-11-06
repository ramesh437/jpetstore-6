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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state (account, cart, order) into Google Cloud Memorystore for Redis so that application
 * instances stay stateless and can be scaled horizontally.
 * <p>
 * Connection settings are read from environment variables (or JVM system properties of the same name):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance IP/host (default {@code localhost})</li>
 * <li>{@code REDIS_PORT} - Memorystore port (default {@code 6379})</li>
 * <li>{@code REDIS_AUTH} - Memorystore AUTH string (optional)</li>
 * <li>{@code SESSION_TIMEOUT_SECONDS} - session TTL in seconds (default {@code 1800})</li>
 * <li>{@code SESSION_REDIS_NAMESPACE} - Redis key namespace (default {@code jpetstore:session})</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  private static String setting(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value == null || value.isEmpty() ? defaultValue : value;
  }

  /**
   * Redis connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(setting("REDIS_HOST", "localhost"),
        Integer.parseInt(setting("REDIS_PORT", "6379")));
    String auth = setting("REDIS_AUTH", null);
    if (auth != null) {
      config.setPassword(auth);
    }
    return new LettuceConnectionFactory(config);
  }
}
