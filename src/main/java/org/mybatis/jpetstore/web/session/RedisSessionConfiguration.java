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
package org.mybatis.jpetstore.web.session;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Configuration of the Memorystore for Redis backed session state. Everything is configured through environment
 * variables (REDIS_HOST, REDIS_PORT, REDIS_PASSWORD / REDIS_AUTH, REDIS_SSL_ENABLED, SESSION_TTL_SECONDS,
 * SESSION_KEY_PREFIX).
 */
@Configuration
public class RedisSessionConfiguration {

  /**
   * Redis connection factory.
   *
   * @param host
   *          the Memorystore host
   * @param port
   *          the Memorystore port
   * @param password
   *          the AUTH string (optional)
   * @param sslEnabled
   *          whether in-transit encryption is enabled
   *
   * @return the connection factory
   */
  @Bean(destroyMethod = "destroy")
  public LettuceConnectionFactory redisConnectionFactory(@Value("${REDIS_HOST:localhost}") String host,
      @Value("${REDIS_PORT:6379}") int port, @Value("${REDIS_PASSWORD:${REDIS_AUTH:}}") String password,
      @Value("${REDIS_SSL_ENABLED:false}") boolean sslEnabled) {
    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
    if (password != null && !password.isEmpty()) {
      serverConfig.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.ofSeconds(2));
    if (sslEnabled) {
      clientConfig.useSsl();
    }
    return new LettuceConnectionFactory(serverConfig, clientConfig.build());
  }

  /**
   * Session state store.
   *
   * @param connectionFactory
   *          the connection factory
   * @param ttlSeconds
   *          the sliding session timeout in seconds
   * @param keyPrefix
   *          the redis key prefix
   *
   * @return the session state store
   */
  @Bean
  public SessionStateStore sessionStateStore(LettuceConnectionFactory connectionFactory,
      @Value("${SESSION_TTL_SECONDS:1800}") long ttlSeconds,
      @Value("${SESSION_KEY_PREFIX:jpetstore:session:}") String keyPrefix) {
    return new RedisSessionStateStore(connectionFactory, keyPrefix, Duration.ofSeconds(ttlSeconds));
  }
}
