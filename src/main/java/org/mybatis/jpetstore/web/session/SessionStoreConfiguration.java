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
import java.util.logging.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configures the external session state store (Google Cloud Memorystore for Redis).
 * <p>
 * Environment variables: REDIS_HOST (Memorystore primary endpoint IP/host), REDIS_PORT (default 6379), REDIS_PASSWORD
 * (Memorystore AUTH string, optional), REDIS_SSL (true to enable in-transit encryption, default false),
 * SESSION_TTL_SECONDS (default 1800).
 */
@Configuration
public class SessionStoreConfiguration {

  private static final Logger LOGGER = Logger.getLogger(SessionStoreConfiguration.class.getName());

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  private static Duration sessionTtl() {
    return Duration.ofSeconds(Long.parseLong(env("SESSION_TTL_SECONDS", "1800")));
  }

  /**
   * Creates the Redis connection factory (Lettuce). Connections are established lazily.
   *
   * @return the connection factory, or null when REDIS_HOST is not configured
   */
  private static LettuceConnectionFactory createConnectionFactory() {
    String host = env("REDIS_HOST", "");
    if (host.isEmpty()) {
      return null;
    }
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host,
        Integer.parseInt(env("REDIS_PORT", "6379")));
    String password = env("REDIS_PASSWORD", "");
    if (!password.isEmpty()) {
      configuration.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.ofSeconds(Long.parseLong(env("REDIS_TIMEOUT_SECONDS", "2"))));
    if (Boolean.parseBoolean(env("REDIS_SSL", "false"))) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(configuration, builder.build());
  }

  /**
   * Session state store.
   *
   * @return the session state store
   */
  @Bean(destroyMethod = "destroy")
  public SessionStateStore sessionStateStore() {
    LettuceConnectionFactory connectionFactory = createConnectionFactory();
    if (connectionFactory == null) {
      LOGGER.warning("REDIS_HOST is not set: using LOCAL in-memory session store. Do not use this in a "
          + "multi-instance deployment; configure Memorystore for Redis via REDIS_HOST.");
      return new LocalSessionStateStore(sessionTtl());
    }
    connectionFactory.afterPropertiesSet();
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer(
        getClass().getClassLoader());
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);
    template.afterPropertiesSet();
    return new RedisSessionStateStore(template, sessionTtl(), connectionFactory);
  }
}
