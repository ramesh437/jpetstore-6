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

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.data.redis.RedisSessionRepository;

/**
 * Externalises the HTTP session state (account, cart, order) so that the application instances are stateless.
 * <p>
 * When {@code REDIS_HOST} is defined (GCP Memorystore for Redis), sessions are stored in Redis and shared by all
 * instances. Without {@code REDIS_HOST} (local development / unit tests) an in-memory repository is used.
 * </p>
 * <p>
 * Environment variables:
 * </p>
 * <ul>
 * <li>{@code REDIS_HOST} - Memorystore instance host/IP</li>
 * <li>{@code REDIS_PORT} - port (default 6379)</li>
 * <li>{@code REDIS_AUTH} - AUTH string, if enabled on the instance (optional)</li>
 * <li>{@code REDIS_SSL} - {@code true} to enable in-transit encryption (optional)</li>
 * <li>{@code SESSION_TIMEOUT_SECONDS} - session idle timeout (default 1800, Redis only)</li>
 * <li>{@code SESSION_REDIS_NAMESPACE} - Redis key prefix (default {@code jpetstore:session})</li>
 * </ul>
 */
@Configuration
public class SessionConfig {

  /** Environment variable holding the Memorystore host. */
  static final String REDIS_HOST = "REDIS_HOST";

  /**
   * Condition: Memorystore for Redis is configured.
   */
  static class RedisConfiguredCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      String host = context.getEnvironment().getProperty(REDIS_HOST);
      return host != null && !host.trim().isEmpty();
    }
  }

  /**
   * Condition: Memorystore for Redis is not configured.
   */
  static class RedisNotConfiguredCondition extends RedisConfiguredCondition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !super.matches(context, metadata);
    }
  }

  private static int timeoutSeconds(Environment env) {
    return env.getProperty("SESSION_TIMEOUT_SECONDS", Integer.class, MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);
  }

  /**
   * Redis (Memorystore) backed session store.
   */
  @Configuration
  @EnableSpringHttpSession
  @Conditional(RedisConfiguredCondition.class)
  static class MemorystoreSessionConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(Environment env) {
      RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(env.getProperty(REDIS_HOST),
          env.getProperty("REDIS_PORT", Integer.class, 6379));
      String auth = env.getProperty("REDIS_AUTH");
      if (auth != null && !auth.isEmpty()) {
        server.setPassword(RedisPassword.of(auth));
      }
      LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
          .commandTimeout(Duration.ofSeconds(5));
      if (env.getProperty("REDIS_SSL", Boolean.class, Boolean.FALSE)) {
        client.useSsl();
      }
      return new LettuceConnectionFactory(server, client.build());
    }

    @Bean
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
      RedisTemplate<String, Object> template = new RedisTemplate<>();
      template.setKeySerializer(new StringRedisSerializer());
      template.setHashKeySerializer(new StringRedisSerializer());
      // values use JDK serialization: session attributes (AccountSession, Cart, Order) are Serializable
      template.setConnectionFactory(connectionFactory);
      return template;
    }

    @Bean
    public RedisSessionRepository sessionRepository(RedisTemplate<String, Object> sessionRedisTemplate,
        Environment env) {
      RedisSessionRepository repository = new RedisSessionRepository(sessionRedisTemplate);
      repository.setDefaultMaxInactiveInterval(Duration.ofSeconds(timeoutSeconds(env)));
      repository.setRedisKeyNamespace(env.getProperty("SESSION_REDIS_NAMESPACE", "jpetstore:session"));
      return repository;
    }
  }

  /**
   * Local fallback (development / tests only).
   */
  @Configuration
  @EnableSpringHttpSession
  @Conditional(RedisNotConfiguredCondition.class)
  static class LocalSessionConfig {

    @Bean
    public MapSessionRepository sessionRepository() {
      return new MapSessionRepository(new ConcurrentHashMap<>());
    }
  }
}
