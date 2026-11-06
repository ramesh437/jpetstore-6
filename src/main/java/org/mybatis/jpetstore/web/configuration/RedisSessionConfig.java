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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state (account, cart, order) to Google Cloud Memorystore for Redis so that the
 * application instances are stateless and can be scaled horizontally.
 * <p>
 * Connection settings are read from environment variables / system properties:
 * <ul>
 * <li>REDIS_HOST (default: localhost) - Memorystore instance IP / host</li>
 * <li>REDIS_PORT (default: 6379)</li>
 * <li>REDIS_PASSWORD (optional) - AUTH string, e.g. injected from Secret Manager</li>
 * <li>REDIS_SSL (default: false) - enable TLS (Memorystore in-transit encryption)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  @Autowired
  private Environment env;

  /**
   * Redis connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(env.getProperty("REDIS_HOST", "localhost"),
        Integer.parseInt(env.getProperty("REDIS_PORT", "6379")));
    String password = env.getProperty("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      config.setPassword(password);
    }
    LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
    factory.setUseSsl(Boolean.parseBoolean(env.getProperty("REDIS_SSL", "false")));
    return factory;
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable automatic keyspace notification configuration.
   *
   * @return a no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
