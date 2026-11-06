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
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes the HTTP session state to Google Cloud Memorystore for Redis so that the application instances stay
 * stateless and can be scaled horizontally.
 *
 * <p>
 * Connection settings are read from environment variables (12-factor):
 * </p>
 * <ul>
 * <li>{@code REDIS_HOST} (default {@code localhost}) - the Memorystore instance IP / host</li>
 * <li>{@code REDIS_PORT} (default {@code 6379})</li>
 * <li>{@code REDIS_PASSWORD} (optional) - the Memorystore AUTH string</li>
 * <li>{@code REDIS_SSL} (default {@code false}) - set to {@code true} for in-transit encryption</li>
 * <li>Session timeout is 1800 seconds and the Redis namespace is {@code jpetstore:session}</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfiguration {

  /**
   * Redis connection factory pointing to Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(env("REDIS_HOST", "localhost"),
        Integer.parseInt(env("REDIS_PORT", "6379")));
    String password = System.getenv("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      redisConfig.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(env("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(redisConfig, builder.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable the automatic keyspace notification configuration.
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
