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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Connection settings for Google Cloud Memorystore for Redis, resolved from environment variables:
 * <ul>
 * <li>REDIS_HOST (default localhost) - Memorystore instance IP / host</li>
 * <li>REDIS_PORT (default 6379)</li>
 * <li>REDIS_PASSWORD (optional, e.g. injected from Secret Manager - Memorystore AUTH string)</li>
 * <li>REDIS_SSL (default false) - set to true for Memorystore in-transit encryption</li>
 * </ul>
 */
@Configuration
public class RedisSessionConfig {

  @Value("${REDIS_HOST:localhost}")
  private String host;

  @Value("${REDIS_PORT:6379}")
  private int port;

  @Value("${REDIS_PASSWORD:}")
  private String password;

  @Value("${REDIS_SSL:false}")
  private boolean ssl;

  /**
   * Redis connection factory.
   *
   * @return the connection factory
   */
  @Bean(destroyMethod = "destroy")
  public LettuceConnectionFactory sessionRedisConnectionFactory() {
    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
    if (password != null && !password.isEmpty()) {
      serverConfig.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration clientConfig = ssl
        ? LettuceClientConfiguration.builder().useSsl().and().commandTimeout(Duration.ofSeconds(2)).build()
        : LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(2)).build();
    return new LettuceConnectionFactory(serverConfig, clientConfig);
  }

  /**
   * Redis template used to store user state.
   *
   * @param sessionRedisConnectionFactory
   *          the connection factory
   *
   * @return the redis template
   */
  @Bean
  public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory sessionRedisConnectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(sessionRedisConnectionFactory);
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer(
        getClass().getClassLoader());
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);
    return template;
  }
}
