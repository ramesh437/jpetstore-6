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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes the HTTP session to Google Cloud Memorystore for Redis so that the application is stateless and can be
 * scaled horizontally. All {@code HttpSession} attributes (account, cart, order) are stored in Redis through Spring
 * Session instead of in the memory of the servlet container.
 *
 * <p>
 * Configuration is read from environment variables (12-factor):
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance host (default: localhost)</li>
 * <li>{@code REDIS_PORT} - Memorystore instance port (default: 6379)</li>
 * <li>{@code REDIS_PASSWORD} - AUTH string (optional)</li>
 * <li>{@code REDIS_SSL} - set to {@code true} to enable in-transit encryption (optional)</li>
 * <li>{@code SESSION_TIMEOUT_SECONDS} - session inactivity timeout (default: 1800)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  /**
   * Lettuce connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(env("REDIS_HOST", "localhost"),
        Integer.parseInt(env("REDIS_PORT", "6379")));
    String password = env("REDIS_PASSWORD", "");
    if (!password.isEmpty()) {
      config.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (Boolean.parseBoolean(env("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Memorystore does not allow the CONFIG command, so disable Spring Session's automatic keyspace notification setup.
   *
   * @return a no-op action
   */
  @Bean
  public static ConfigureRedisAction configureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }

  /**
   * Customizes the session inactivity timeout from the environment.
   *
   * @return the repository customizer
   */
  @Bean
  public org.springframework.session.config.SessionRepositoryCustomizer<org.springframework.session.data.redis.RedisIndexedSessionRepository> sessionTimeoutCustomizer() {
    int timeout = Integer.parseInt(env("SESSION_TIMEOUT_SECONDS", "1800"));
    return repository -> repository.setDefaultMaxInactiveInterval(timeout);
  }
}
