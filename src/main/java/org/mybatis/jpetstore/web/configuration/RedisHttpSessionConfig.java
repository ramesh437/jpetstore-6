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
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis HTTP Session Configuration for GCP Memorystore.
 *
 * <p>Replaces the default in-memory HTTP session storage with Google Cloud Memorystore for Redis,
 * enabling stateless application architecture with centralized session management across
 * distributed instances. This eliminates server affinity and supports horizontal scaling.
 *
 * <p>Session data (account info, shopping cart) previously stored in {@code HttpSession} via
 * {@code session.setAttribute()} is now transparently persisted to Redis through Spring Session,
 * without requiring changes to the controller code that reads/writes session attributes.
 *
 * <p>Configure the Redis host and port via environment variables:
 * <ul>
 *   <li>{@code REDIS_HOST} - GCP Memorystore Redis instance IP (default: localhost)</li>
 *   <li>{@code REDIS_PORT} - GCP Memorystore Redis port (default: 6379)</li>
 *   <li>{@code SESSION_TIMEOUT_SECONDS} - Session timeout in seconds (default: 1800)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisHttpSessionConfig {

  /** Redis host - set to GCP Memorystore instance IP via environment variable REDIS_HOST. */
  @Value("${redis.host:${REDIS_HOST:localhost}}")
  private String redisHost;

  /** Redis port - GCP Memorystore default port via environment variable REDIS_PORT. */
  @Value("${redis.port:${REDIS_PORT:6379}}")
  private int redisPort;

  /**
   * Creates a Lettuce connection factory pointing to GCP Memorystore for Redis.
   *
   * <p>Lettuce is the recommended Redis client for Spring Session on GCP Memorystore.
   * The connection is configured via {@code REDIS_HOST} and {@code REDIS_PORT} environment
   * variables, which should be set to the GCP Memorystore instance's IP address and port.
   *
   * @return the Lettuce connection factory
   */
  @Bean
  public LettuceConnectionFactory connectionFactory() {
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
    return new LettuceConnectionFactory(redisConfig);
  }
}
