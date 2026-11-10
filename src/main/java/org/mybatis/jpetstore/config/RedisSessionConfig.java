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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis so that application instances stay stateless.
 * <p>
 * Configuration (environment variables / system properties):
 * <ul>
 * <li>REDIS_HOST (default: localhost) - Memorystore instance IP / hostname</li>
 * <li>REDIS_PORT (default: 6379)</li>
 * <li>REDIS_PASSWORD (optional) - Memorystore AUTH string</li>
 * <li>REDIS_SSL (default: false) - set to true for Memorystore in-transit encryption</li>
 * <li>SESSION_NAMESPACE (default: jpetstore:session)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "${SESSION_NAMESPACE:jpetstore:session}")
public class RedisSessionConfig {

  /**
   * Creates the Redis connection factory from environment-provided settings.
   *
   * @param env
   *          the environment
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory(Environment env) {
    RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(env.getProperty("REDIS_HOST", "localhost"),
        env.getProperty("REDIS_PORT", Integer.class, 6379));
    String password = env.getProperty("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      server.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (env.getProperty("REDIS_SSL", Boolean.class, Boolean.FALSE)) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(server, builder.build());
  }

  /**
   * Memorystore does not permit the CONFIG command, so disable Spring Session's automatic keyspace notification
   * configuration.
   *
   * @return a no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
