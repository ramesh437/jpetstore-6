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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Stores the HTTP session (account, cart and order state) in Google Cloud Memorystore for Redis so that the
 * application instances stay stateless and can scale horizontally.
 *
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance IP / host (default {@code localhost})</li>
 * <li>{@code REDIS_PORT} - Redis port (default {@code 6379})</li>
 * <li>{@code REDIS_PASSWORD} - Redis AUTH string (optional, e.g. from Secret Manager)</li>
 * <li>{@code REDIS_SSL} - {@code true} to enable in-transit encryption (default {@code false})</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfiguration {

  /** The environment. */
  @Autowired
  private Environment env;

  /**
   * Redis connection factory pointing at Memorystore.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(env.getProperty("REDIS_HOST", "localhost"),
        Integer.parseInt(env.getProperty("REDIS_PORT", "6379")));
    String password = env.getProperty("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      config.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(env.getProperty("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable automatic keyspace notification configuration.
   *
   * @return the no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
