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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state (account, cart and order) to Google Cloud Memorystore for Redis so that the
 * application instances are stateless and can be scaled horizontally.
 * <p>
 * Connection settings are read from environment variables / system properties:
 * </p>
 * <ul>
 * <li>REDIS_HOST (default: localhost) - Memorystore instance IP / host</li>
 * <li>REDIS_PORT (default: 6379)</li>
 * <li>REDIS_PASSWORD (optional) - Memorystore AUTH string</li>
 * <li>REDIS_SSL (default: false) - set to true when in-transit encryption is enabled</li>
 * <li>SESSION_TIMEOUT_SECONDS (default: 1800)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  /**
   * Creates the Redis connection factory pointing to Memorystore for Redis.
   *
   * @param host
   *          the redis host
   * @param port
   *          the redis port
   * @param password
   *          the redis AUTH password (may be empty)
   * @param ssl
   *          whether to use TLS
   *
   * @return the connection factory
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory(@Value("${REDIS_HOST:localhost}") String host,
      @Value("${REDIS_PORT:6379}") int port, @Value("${REDIS_PASSWORD:}") String password,
      @Value("${REDIS_SSL:false}") boolean ssl) {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
    if (password != null && !password.isEmpty()) {
      config.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (ssl) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable Spring Session's automatic keyspace notification setup.
   *
   * @return the no-op configure action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
