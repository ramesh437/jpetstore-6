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
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis using Spring Session.
 * <p>
 * All {@code HttpSession} usage in the controllers (cart, order, account) is transparently stored in Redis, so any
 * application instance can serve any request (stateless application tier).
 * </p>
 * <p>
 * Configuration (environment variables / system properties):
 * </p>
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance IP/host (default: localhost)</li>
 * <li>{@code REDIS_PORT} - port (default: 6379)</li>
 * <li>{@code REDIS_PASSWORD} - AUTH string, optional</li>
 * <li>{@code REDIS_SSL} - set to true to enable in-transit encryption (default: false)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, saveMode = SaveMode.ALWAYS, flushMode = FlushMode.ON_SAVE, redisNamespace = "jpetstore:session")
public class RedisSessionConfiguration {

  /** The Memorystore host. */
  @Value("${REDIS_HOST:localhost}")
  private String redisHost;

  /** The Memorystore port. */
  @Value("${REDIS_PORT:6379}")
  private int redisPort;

  /** The Memorystore AUTH string (optional). */
  @Value("${REDIS_PASSWORD:}")
  private String redisPassword;

  /** Whether to use TLS (Memorystore in-transit encryption). */
  @Value("${REDIS_SSL:false}")
  private boolean redisSsl;

  /**
   * Redis connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
    if (redisPassword != null && !redisPassword.isEmpty()) {
      config.setPassword(redisPassword);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (redisSsl) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Memorystore does not permit the CONFIG command, so disable Spring Session's automatic keyspace notification setup.
   * Configure {@code notify-keyspace-events} on the Memorystore instance if session expiry events are required.
   *
   * @return a no-op redis action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
