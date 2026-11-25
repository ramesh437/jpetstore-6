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
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Stores HTTP session state in Google Cloud Memorystore for Redis so that application instances are stateless.
 * <p>
 * Configuration (12-factor, environment variables or system properties):
 * <ul>
 * <li>REDIS_HOST (default localhost) - Memorystore instance IP/host</li>
 * <li>REDIS_PORT (default 6379)</li>
 * <li>REDIS_AUTH - Memorystore AUTH string (optional)</li>
 * <li>REDIS_SSL - "true" to enable in-transit encryption (optional)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  @Autowired
  private Environment env;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(env.getProperty("REDIS_HOST", "localhost"),
        Integer.parseInt(env.getProperty("REDIS_PORT", "6379")));
    String auth = env.getProperty("REDIS_AUTH");
    if (auth != null && !auth.isEmpty()) {
      config.setPassword(RedisPassword.of(auth));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(env.getProperty("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Memorystore does not permit the CONFIG command, so disable Spring Session's keyspace-notification setup.
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
