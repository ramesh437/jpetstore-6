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

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Session state store backed by Google Cloud Memorystore for Redis.
 */
public class RedisSessionStateStore implements SessionStateStore {

  /** The Redis key prefix. */
  private static final String KEY_PREFIX = "jpetstore:session:";

  /** The redis template. */
  private final RedisTemplate<String, Object> redisTemplate;
  /** The session time to live. */
  private final Duration ttl;
  /** The connection factory owned by this store. */
  private final LettuceConnectionFactory connectionFactory;

  /**
   * Instantiates a new redis session state store.
   *
   * @param redisTemplate
   *          the redis template
   * @param ttl
   *          the session time to live
   * @param connectionFactory
   *          the connection factory to release on shutdown
   */
  public RedisSessionStateStore(RedisTemplate<String, Object> redisTemplate, Duration ttl,
      LettuceConnectionFactory connectionFactory) {
    this.redisTemplate = redisTemplate;
    this.ttl = ttl;
    this.connectionFactory = connectionFactory;
  }

  /**
   * Releases Redis connections.
   */
  public void destroy() {
    connectionFactory.destroy();
  }

  @Override
  public Object get(String sessionId, String key) {
    String redisKey = KEY_PREFIX + sessionId;
    Object value = redisTemplate.opsForHash().get(redisKey, key);
    if (value != null) {
      redisTemplate.expire(redisKey, ttl);
    }
    return value;
  }

  @Override
  public void put(String sessionId, String key, Object value) {
    String redisKey = KEY_PREFIX + sessionId;
    if (value == null) {
      redisTemplate.opsForHash().delete(redisKey, key);
      return;
    }
    redisTemplate.opsForHash().put(redisKey, key, value);
    redisTemplate.expire(redisKey, ttl);
  }

  @Override
  public void remove(String sessionId) {
    redisTemplate.delete(KEY_PREFIX + sessionId);
  }
}
