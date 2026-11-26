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
package org.mybatis.jpetstore.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig — configures a Spring Data Redis {@link RedisTemplate} backed by
 * Google Cloud Memorystore for Redis (Jedis client).
 *
 * <p>Connection parameters are read from environment variables / system
 * properties so that no credentials or host addresses are hard-coded in source:
 * <ul>
 *   <li>{@code REDIS_HOST} — Memorystore instance IP or hostname (default: {@code localhost})</li>
 *   <li>{@code REDIS_PORT} — Memorystore port (default: {@code 6379})</li>
 *   <li>{@code REDIS_PASSWORD} — optional AUTH password (default: empty)</li>
 * </ul>
 *
 * <p>Values are serialised as JSON using {@link GenericJackson2JsonRedisSerializer}
 * so that complex domain objects (e.g. {@code Cart}, {@code AccountSession}) can
 * be stored and retrieved across distributed application instances.
 */
@Configuration
public class RedisConfig {

  /** Memorystore host — injected from the {@code REDIS_HOST} environment variable. */
  @Value("${REDIS_HOST:localhost}")
  private String redisHost;

  /** Memorystore port — injected from the {@code REDIS_PORT} environment variable. */
  @Value("${REDIS_PORT:6379}")
  private int redisPort;

  /** Optional Redis AUTH password — injected from the {@code REDIS_PASSWORD} environment variable. */
  @Value("${REDIS_PASSWORD:}")
  private String redisPassword;

  /**
   * Creates a Jedis-based {@link RedisConnectionFactory} pointing at the
   * Google Cloud Memorystore instance whose coordinates are supplied via
   * environment variables.
   *
   * @return the configured {@link RedisConnectionFactory}
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
    if (redisPassword != null && !redisPassword.isEmpty()) {
      config.setPassword(redisPassword);
    }
    return new JedisConnectionFactory(config);
  }

  /**
   * Creates a {@link RedisTemplate} with:
   * <ul>
   *   <li>String keys serialised with {@link StringRedisSerializer}</li>
   *   <li>Object values serialised as JSON with {@link GenericJackson2JsonRedisSerializer}</li>
   * </ul>
   *
   * @param connectionFactory the Redis connection factory
   * @return the configured {@link RedisTemplate}
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }
}
