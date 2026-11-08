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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis so that the application instances are
 * stateless and can be scaled horizontally.
 * <p>
 * Connection settings are read from environment variables (12-factor): {@code REDIS_HOST}, {@code REDIS_PORT},
 * {@code REDIS_PASSWORD} (optional AUTH string), {@code SESSION_TIMEOUT_SECONDS}.
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfiguration {

  @Value("${REDIS_HOST:localhost}")
  private String redisHost;

  @Value("${REDIS_PORT:6379}")
  private int redisPort;

  @Value("${REDIS_PASSWORD:}")
  private String redisPassword;

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
    return new LettuceConnectionFactory(config);
  }

  /**
   * Memorystore does not allow the CONFIG command used by Spring Session to enable keyspace notifications, so disable
   * the automatic configuration.
   *
   * @return a no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
