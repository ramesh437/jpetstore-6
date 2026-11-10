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
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis so that application instances stay stateless.
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance IP / host (default {@code localhost})</li>
 * <li>{@code REDIS_PORT} - Memorystore port (default {@code 6379})</li>
 * <li>{@code REDIS_PASSWORD} - Memorystore AUTH string (optional)</li>
 * <li>{@code REDIS_SSL_ENABLED} - enable in-transit encryption (default {@code false})</li>
 * </ul>
 * All session attributes (e.g. {@code accountBean}, {@code cart}, {@code order}) are stored in Redis through the
 * standard {@code HttpSession} API, so JSP {@code sessionScope} references keep working.
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "jpetstore:session", maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {

  /** The environment. */
  @Autowired
  private Environment env;

  /**
   * Redis connection factory pointing at Memorystore.
   *
   * @return the lettuce connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    String host = env.getProperty("REDIS_HOST", "localhost");
    int port = Integer.parseInt(env.getProperty("REDIS_PORT", "6379"));
    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
    String password = env.getProperty("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      serverConfig.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(env.getProperty("REDIS_SSL_ENABLED", "false"))) {
      clientConfig.useSsl();
    }
    return new LettuceConnectionFactory(serverConfig, clientConfig.build());
  }

  /**
   * Memorystore does not permit the CONFIG command, so disable Spring Session's keyspace-notification auto config.
   *
   * @return the no-op configure action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
