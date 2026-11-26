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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * RedisSessionService — centralises all HTTP-session state reads and writes
 * against Google Cloud Memorystore for Redis, enabling stateless application
 * instances that can be horizontally scaled without server affinity.
 *
 * <p>Each logical session attribute is stored under a composite Redis key of
 * the form {@code session:<sessionId>:<attributeName>} with a configurable TTL
 * so that stale data is automatically evicted by Redis.
 */
@Service
public class RedisSessionService {

  private static final Logger logger = LoggerFactory.getLogger(RedisSessionService.class);

  /** Redis key prefix for all session entries. */
  private static final String SESSION_KEY_PREFIX = "session:";

  /** Default session TTL in seconds (30 minutes). */
  @Value("${redis.session.ttl.seconds:1800}")
  private long sessionTtlSeconds;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  // -------------------------------------------------------------------------
  // Generic attribute accessors
  // -------------------------------------------------------------------------

  /**
   * Stores a session attribute in Redis and resets the TTL.
   *
   * @param sessionId     the HTTP session identifier
   * @param attributeName the attribute name (e.g. "accountBean", "cart")
   * @param value         the serialisable value to store
   */
  public void setAttribute(String sessionId, String attributeName, Object value) {
    String key = buildKey(sessionId, attributeName);
    redisTemplate.opsForValue().set(key, value, sessionTtlSeconds, TimeUnit.SECONDS);
    logger.debug("Redis session attribute set: key={}", key);
  }

  /**
   * Retrieves a session attribute from Redis.
   *
   * @param sessionId     the HTTP session identifier
   * @param attributeName the attribute name
   * @return the stored value, or {@code null} if absent / expired
   */
  public Object getAttribute(String sessionId, String attributeName) {
    String key = buildKey(sessionId, attributeName);
    Object value = redisTemplate.opsForValue().get(key);
    if (value != null) {
      // Refresh TTL on access (sliding expiry)
      redisTemplate.expire(key, sessionTtlSeconds, TimeUnit.SECONDS);
    }
    return value;
  }

  /**
   * Removes a single session attribute from Redis.
   *
   * @param sessionId     the HTTP session identifier
   * @param attributeName the attribute name
   */
  public void removeAttribute(String sessionId, String attributeName) {
    String key = buildKey(sessionId, attributeName);
    redisTemplate.delete(key);
    logger.debug("Redis session attribute removed: key={}", key);
  }

  /**
   * Invalidates the entire session by deleting all known attribute keys.
   * Uses a Redis SCAN to find all keys matching the session prefix pattern.
   *
   * @param sessionId the HTTP session identifier
   */
  public void invalidateSession(String sessionId) {
    String pattern = SESSION_KEY_PREFIX + sessionId + ":*";
    java.util.Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      logger.debug("Redis session invalidated: sessionId={}, keysDeleted={}", sessionId, keys.size());
    }
  }

  // -------------------------------------------------------------------------
  // Typed convenience accessors
  // -------------------------------------------------------------------------

  /**
   * Typed getter — casts the stored value to the requested type.
   *
   * @param sessionId     the HTTP session identifier
   * @param attributeName the attribute name
   * @param type          the expected type
   * @param <T>           the type parameter
   * @return the typed value, or {@code null} if absent
   */
  @SuppressWarnings("unchecked")
  public <T> T getAttribute(String sessionId, String attributeName, Class<T> type) {
    Object value = getAttribute(sessionId, attributeName);
    if (value == null) {
      return null;
    }
    if (type.isInstance(value)) {
      return (T) value;
    }
    logger.warn("Redis session attribute type mismatch: key={}, expected={}, actual={}",
        buildKey(sessionId, attributeName), type.getName(), value.getClass().getName());
    return null;
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private String buildKey(String sessionId, String attributeName) {
    return SESSION_KEY_PREFIX + sessionId + ":" + attributeName;
  }
}
