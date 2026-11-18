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

import java.io.Serializable;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Session state store backed by Google Cloud Memorystore for Redis. Each session is a Redis hash with a sliding
 * expiration (SESSION_TIMEOUT_SECONDS, default 1800).
 */
@Component
public class RedisSessionStateStore implements SessionStateStore {

  /** Redis key prefix. */
  private static final String KEY_PREFIX = "jpetstore:session:";

  /** The redis template. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** Session time to live. */
  private final Duration timeout;

  /**
   * Instantiates a new redis session state store.
   *
   * @param redisTemplate
   *          the redis template
   * @param timeoutSeconds
   *          the session timeout in seconds
   */
  @Autowired
  public RedisSessionStateStore(RedisTemplate<String, Object> redisTemplate,
      @Value("${SESSION_TIMEOUT_SECONDS:1800}") long timeoutSeconds) {
    this.redisTemplate = redisTemplate;
    this.timeout = Duration.ofSeconds(timeoutSeconds);
  }

  @Override
  public <T> T get(String sessionId, String key, Class<T> type) {
    String redisKey = KEY_PREFIX + sessionId;
    Object value = redisTemplate.opsForHash().get(redisKey, key);
    if (value == null) {
      return null;
    }
    redisTemplate.expire(redisKey, timeout);
    return type.cast(value);
  }

  @Override
  public void put(String sessionId, String key, Serializable value) {
    String redisKey = KEY_PREFIX + sessionId;
    redisTemplate.opsForHash().put(redisKey, key, value);
    redisTemplate.expire(redisKey, timeout);
  }

  @Override
  public void invalidate(String sessionId) {
    redisTemplate.delete(KEY_PREFIX + sessionId);
  }
}
