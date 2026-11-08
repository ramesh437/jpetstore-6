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
package org.mybatis.jpetstore.web.session;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Stores the HTTP session (account, cart and order state) in Google Cloud Memorystore for Redis so that the
 * application instances stay stateless and can be scaled horizontally.
 * <p>
 * Configuration is read from environment variables (or JVM system properties of the same name):
 * </p>
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance host (default {@code localhost})</li>
 * <li>{@code REDIS_PORT} - Memorystore instance port (default {@code 6379})</li>
 * <li>{@code REDIS_AUTH_STRING} (or {@code REDIS_PASSWORD}) - AUTH string, optional</li>
 * <li>{@code REDIS_SSL_ENABLED} - {@code true} to enable in-transit encryption, optional</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  /**
   * Redis connection factory pointing at Memorystore.
   *
   * @return the connection factory
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(getSetting("REDIS_HOST", "localhost"),
        Integer.parseInt(getSetting("REDIS_PORT", "6379")));
    String password = getSetting("REDIS_AUTH_STRING", getSetting("REDIS_PASSWORD", null));
    if (password != null && !password.isEmpty()) {
      config.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(getSetting("REDIS_SSL_ENABLED", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable Spring Session's keyspace notification setup.
   * Keyspace notifications can be enabled on the Memorystore instance through its redis-config settings.
   *
   * @return the no-op action
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
