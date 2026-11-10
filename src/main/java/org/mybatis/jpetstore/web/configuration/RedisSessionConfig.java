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
 * Externalizes HTTP session state (account, cart, order) to Google Cloud Memorystore for Redis so that the application
 * instances stay stateless and can be scaled horizontally.
 * <p>
 * Connection settings are read from environment variables (12-factor):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance host (default {@code localhost})</li>
 * <li>{@code REDIS_PORT} - Memorystore instance port (default {@code 6379})</li>
 * <li>{@code REDIS_PASSWORD} - Memorystore AUTH string (optional; inject from Secret Manager)</li>
 * <li>{@code REDIS_SSL} - {@code true} to enable in-transit encryption (default {@code false})</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Lettuce connection factory pointing to Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(env("REDIS_HOST", "localhost"),
        Integer.parseInt(env("REDIS_PORT", "6379")));
    String password = env("REDIS_PASSWORD", null);
    if (password != null) {
      serverConfig.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(env("REDIS_SSL", "false"))) {
      clientConfig.useSsl();
    }
    return new LettuceConnectionFactory(serverConfig, clientConfig.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable Spring Session's automatic keyspace notification setup.
   * Enable keyspace notifications ({@code notify-keyspace-events gxE}) through the instance's redis-configs if session
   * expiry events are required.
   *
   * @return a no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
