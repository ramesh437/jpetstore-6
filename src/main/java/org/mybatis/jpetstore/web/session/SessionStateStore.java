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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis (Google Cloud Memorystore) backed store for per-user state (account, cart, order).
 * <p>
 * Each user's state is kept as a Redis hash under {@code jpetstore:session:<id>} with a sliding expiration, so any
 * application instance can serve any request and no state is held in the servlet container's memory.
 */
@Component
public class SessionStateStore {

  private static final String KEY_PREFIX = "jpetstore:session:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final Duration ttl;

  /**
   * Instantiates a new session state store.
   *
   * @param redisTemplate
   *          the redis template
   * @param ttlMinutes
   *          the state time-to-live in minutes (env var SESSION_TTL_MINUTES)
   */
  @Autowired
  public SessionStateStore(@Qualifier("sessionRedisTemplate") RedisTemplate<String, Object> redisTemplate,
      @Value("${SESSION_TTL_MINUTES:30}") long ttlMinutes) {
    this.redisTemplate = redisTemplate;
    this.ttl = Duration.ofMinutes(ttlMinutes);
  }

  /**
   * Gets a state attribute.
   *
   * @param id
   *          the state id
   * @param name
   *          the attribute name
   *
   * @return the attribute value or null
   */
  public Object get(String id, String name) {
    return redisTemplate.opsForHash().get(key(id), name);
  }

  /**
   * Stores a state attribute and refreshes the expiration.
   *
   * @param id
   *          the state id
   * @param name
   *          the attribute name
   * @param value
   *          the attribute value
   */
  public void put(String id, String name, Object value) {
    String key = key(id);
    redisTemplate.opsForHash().put(key, name, value);
    redisTemplate.expire(key, ttl);
  }

  /**
   * Removes a state attribute.
   *
   * @param id
   *          the state id
   * @param name
   *          the attribute name
   */
  public void remove(String id, String name) {
    redisTemplate.opsForHash().delete(key(id), name);
  }

  /**
   * Refreshes the expiration of the state.
   *
   * @param id
   *          the state id
   */
  public void touch(String id) {
    redisTemplate.expire(key(id), ttl);
  }

  /**
   * Deletes the whole state.
   *
   * @param id
   *          the state id
   */
  public void delete(String id) {
    redisTemplate.delete(key(id));
  }

  /**
   * Moves the state to a new id (used to prevent session fixation on sign on).
   *
   * @param oldId
   *          the old id
   * @param newId
   *          the new id
   */
  public void rename(String oldId, String newId) {
    if (Boolean.TRUE.equals(redisTemplate.hasKey(key(oldId)))) {
      redisTemplate.rename(key(oldId), key(newId));
      redisTemplate.expire(key(newId), ttl);
    }
  }

  private static String key(String id) {
    return KEY_PREFIX + id;
  }
}
