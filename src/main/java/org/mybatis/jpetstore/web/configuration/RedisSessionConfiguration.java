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
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis (Spring Session), so that the application
 * instances stay stateless and can be scaled horizontally.
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>REDIS_HOST - Memorystore instance IP/host (default: localhost)</li>
 * <li>REDIS_PORT - Memorystore port (default: 6379)</li>
 * <li>REDIS_AUTH - AUTH string, inject from Secret Manager (default: none)</li>
 * <li>REDIS_SSL - enable in-transit encryption, "true"/"false" (default: false)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "jpetstore:session", maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfiguration {

  /** The environment. */
  @Autowired
  private Environment env;

  /**
   * Redis connection factory pointing to Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(env.getProperty("REDIS_HOST", "localhost"),
        env.getProperty("REDIS_PORT", Integer.class, 6379));
    String auth = env.getProperty("REDIS_AUTH");
    if (auth != null && !auth.isEmpty()) {
      config.setPassword(RedisPassword.of(auth));
    }
    LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
    factory.setUseSsl(env.getProperty("REDIS_SSL", Boolean.class, Boolean.FALSE));
    return factory;
  }

  /**
   * Memorystore does not permit the CONFIG command, so disable keyspace notification auto-configuration.
   *
   * @return the no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
