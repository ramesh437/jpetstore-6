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

import org.springframework.data.redis.core.RedisTemplate;

/**
 * Session state store backed by Google Cloud Memorystore for Redis. Each client session is a Redis hash with a sliding
 * time-to-live.
 */
public class RedisSessionStateStore extends AbstractCookieSessionStateStore {

  private static final String KEY_PREFIX = "jpetstore:session:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final Duration timeout;

  /**
   * Instantiates a new redis session state store.
   *
   * @param redisTemplate
   *          the redis template
   * @param timeout
   *          the session inactivity timeout
   */
  public RedisSessionStateStore(RedisTemplate<String, Object> redisTemplate, Duration timeout) {
    this.redisTemplate = redisTemplate;
    this.timeout = timeout;
  }

  @Override
  protected Object load(String sid, String name) {
    String key = KEY_PREFIX + sid;
    Object value = redisTemplate.opsForHash().get(key, name);
    if (value != null) {
      redisTemplate.expire(key, timeout);
    }
    return value;
  }

  @Override
  protected void store(String sid, String name, Object value) {
    String key = KEY_PREFIX + sid;
    if (value == null) {
      redisTemplate.opsForHash().delete(key, name);
      return;
    }
    redisTemplate.opsForHash().put(key, name, value);
    redisTemplate.expire(key, timeout);
  }

  @Override
  protected void delete(String sid) {
    redisTemplate.delete(KEY_PREFIX + sid);
  }
}
