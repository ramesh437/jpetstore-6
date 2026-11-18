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
package org.mybatis.jpetstore.session;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Connection settings for Google Cloud Memorystore for Redis, supplied through environment variables (12-factor).
 * <ul>
 * <li>REDIS_HOST - Memorystore instance IP / host (default localhost)</li>
 * <li>REDIS_PORT - port (default 6379)</li>
 * <li>REDIS_PASSWORD - AUTH string (optional, from Secret Manager)</li>
 * <li>REDIS_SSL - true to enable in-transit encryption (default false)</li>
 * <li>REDIS_TIMEOUT_MS - command timeout in milliseconds (default 2000)</li>
 * </ul>
 */
@Configuration
public class RedisSessionConfiguration {

  /** The redis host. */
  @Value("${REDIS_HOST:localhost}")
  private String host;

  /** The redis port. */
  @Value("${REDIS_PORT:6379}")
  private int port;

  /** The redis password. */
  @Value("${REDIS_PASSWORD:}")
  private String password;

  /** Whether to use SSL. */
  @Value("${REDIS_SSL:false}")
  private boolean ssl;

  /** The command timeout in milliseconds. */
  @Value("${REDIS_TIMEOUT_MS:2000}")
  private long timeoutMs;

  /**
   * Redis connection factory.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
    if (password != null && !password.isEmpty()) {
      config.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.ofMillis(timeoutMs));
    if (ssl) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(config, builder.build());
  }

  /**
   * Redis template used to persist session state.
   *
   * @param connectionFactory
   *          the connection factory
   *
   * @return the redis template
   */
  @Bean
  public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new JdkSerializationRedisSerializer());
    template.setHashValueSerializer(new JdkSerializationRedisSerializer());
    return template;
  }
}
