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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configures externalized session state (Memorystore for Redis). Settings come from environment variables:
 * <ul>
 * <li>REDIS_HOST - Memorystore primary endpoint IP/host (required in cloud)</li>
 * <li>REDIS_PORT - default 6379</li>
 * <li>REDIS_PASSWORD - Memorystore AUTH string (optional)</li>
 * <li>REDIS_SSL - "true" to enable in-transit encryption (default false)</li>
 * <li>SESSION_TIMEOUT_SECONDS - inactivity timeout, default 1800</li>
 * </ul>
 */
@Configuration
public class SessionStateConfig implements DisposableBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(SessionStateConfig.class);

  private final Environment env;

  /**
   * Instantiates a new session state config.
   *
   * @param env
   *          the env
   */
  public SessionStateConfig(Environment env) {
    this.env = env;
  }

  private boolean redisConfigured() {
    String host = env.getProperty("REDIS_HOST");
    return host != null && !host.trim().isEmpty();
  }

  private LettuceConnectionFactory connectionFactory;

  /**
   * Releases the Redis connections on shutdown.
   */
  @Override
  public void destroy() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  private LettuceConnectionFactory createConnectionFactory() {
    RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(env.getProperty("REDIS_HOST").trim(),
        env.getProperty("REDIS_PORT", Integer.class, 6379));
    String password = env.getProperty("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      cfg.setPassword(password);
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.ofSeconds(env.getProperty("REDIS_TIMEOUT_SECONDS", Long.class, 5L)));
    if (env.getProperty("REDIS_SSL", Boolean.class, Boolean.FALSE)) {
      builder.useSsl();
    }
    LettuceConnectionFactory factory = new LettuceConnectionFactory(cfg, builder.build());
    factory.afterPropertiesSet();
    return factory;
  }

  /**
   * Session state store.
   *
   * @return the redis backed store (or a local fallback for development when REDIS_HOST is unset)
   */
  @Bean
  public SessionStateStore sessionStateStore() {
    if (!redisConfigured()) {
      LOGGER.warn("REDIS_HOST is not set: using process-local session state. DO NOT use this in a multi-instance "
          + "deployment; configure Memorystore for Redis.");
      return new LocalSessionStateStore();
    }
    connectionFactory = createConnectionFactory();
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    StringRedisSerializer keySerializer = new StringRedisSerializer();
    JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer(
        getClass().getClassLoader());
    template.setKeySerializer(keySerializer);
    template.setHashKeySerializer(keySerializer);
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);
    template.afterPropertiesSet();
    Duration timeout = Duration.ofSeconds(env.getProperty("SESSION_TIMEOUT_SECONDS", Long.class, 1800L));
    return new RedisSessionStateStore(template, timeout);
  }
}
