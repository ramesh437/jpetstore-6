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
package org.mybatis.jpetstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Amazon ElastiCache for Redis using Spring Session, so that application instances
 * are stateless and can be scaled horizontally.
 *
 * <p>
 * Configuration is read from environment variables (or system properties):
 * <ul>
 * <li>REDIS_HOST (default: localhost) - ElastiCache primary endpoint</li>
 * <li>REDIS_PORT (default: 6379)</li>
 * <li>REDIS_PASSWORD (default: empty) - ElastiCache AUTH token</li>
 * <li>REDIS_SSL (default: false) - set to true when in-transit encryption is enabled</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class SessionConfig {

  /** The redis host. */
  @Value("${REDIS_HOST:localhost}")
  private String redisHost;

  /** The redis port. */
  @Value("${REDIS_PORT:6379}")
  private int redisPort;

  /** The redis password. */
  @Value("${REDIS_PASSWORD:}")
  private String redisPassword;

  /** The redis ssl flag. */
  @Value("${REDIS_SSL:false}")
  private boolean redisSsl;

  /**
   * Redis connection factory.
   *
   * @return the lettuce connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
    if (redisPassword != null && !redisPassword.isEmpty()) {
      serverConfig.setPassword(redisPassword);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (redisSsl) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(serverConfig, builder.build());
  }

  /**
   * ElastiCache does not allow the CONFIG command, so disable keyspace notification auto-configuration.
   *
   * @return the configure redis action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
