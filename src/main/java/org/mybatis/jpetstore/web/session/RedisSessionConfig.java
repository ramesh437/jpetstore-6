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

import io.lettuce.core.ClientOptions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes the HTTP session (cart, account, order state) to Google Cloud Memorystore for Redis so that the
 * application instances stay stateless and can be scaled horizontally.
 * <p>
 * Connection settings come from environment variables (12-factor):
 * <ul>
 * <li>{@code REDIS_HOST} (default {@code localhost}) - Memorystore instance IP / hostname</li>
 * <li>{@code REDIS_PORT} (default {@code 6379})</li>
 * <li>{@code REDIS_PASSWORD} (optional) - Memorystore AUTH string, inject from Secret Manager</li>
 * <li>{@code REDIS_SSL} (default {@code false}) - set to {@code true} for Memorystore in-transit encryption</li>
 * <li>{@code SESSION_REDIS_NAMESPACE} (default {@code jpetstore:session})</li>
 * </ul>
 * Session attributes are stored using JDK serialization, therefore all session objects are {@code Serializable}.
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "${SESSION_REDIS_NAMESPACE:jpetstore:session}", maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Lettuce connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(env("REDIS_HOST", "localhost"),
        Integer.parseInt(env("REDIS_PORT", "6379")));
    String password = env("REDIS_PASSWORD", null);
    if (password != null) {
      redisConfig.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
        .clientOptions(ClientOptions.builder().autoReconnect(true).build());
    if (Boolean.parseBoolean(env("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(redisConfig, builder.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable Spring Session's keyspace-notification auto
   * configuration.
   *
   * @return a no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
