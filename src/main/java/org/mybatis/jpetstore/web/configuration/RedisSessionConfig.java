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
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state (account, cart and order beans) to Google Cloud Memorystore for Redis so that the
 * application instances stay stateless and can be scaled horizontally.
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li><code>REDIS_HOST</code> - Memorystore instance IP / host (default: localhost)</li>
 * <li><code>REDIS_PORT</code> - Memorystore port (default: 6379)</li>
 * <li><code>REDIS_PASSWORD</code> - Memorystore AUTH string, inject from Secret Manager (optional)</li>
 * <li><code>REDIS_SSL</code> - set to <code>true</code> to enable in-transit encryption (default: false)</li>
 * <li><code>SESSION_TIMEOUT_SECONDS</code> - session expiry in seconds (default: 1800)</li>
 * <li><code>SESSION_REDIS_NAMESPACE</code> - key namespace (default: jpetstore:session)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Redis connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(env("REDIS_HOST", "localhost"),
        Integer.parseInt(env("REDIS_PORT", "6379")));
    String password = System.getenv("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      config.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(env("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }
}
