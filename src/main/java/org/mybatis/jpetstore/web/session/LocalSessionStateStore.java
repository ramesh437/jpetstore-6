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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded in-memory session state store. Only used for local development / tests when no Redis host is configured
 * (REDIS_HOST). It is NOT suitable for multi-instance deployments.
 */
public class LocalSessionStateStore implements SessionStateStore {

  /** The sessions. */
  private final Cache<String, Map<String, Object>> sessions;

  /**
   * Instantiates a new local session state store.
   *
   * @param ttl
   *          the session time to live
   */
  public LocalSessionStateStore(Duration ttl) {
    this.sessions = CacheBuilder.newBuilder().maximumSize(10_000).expireAfterAccess(ttl).build();
  }

  /**
   * Nothing to release.
   */
  public void destroy() {
    sessions.invalidateAll();
  }

  @Override
  public Object get(String sessionId, String key) {
    Map<String, Object> session = sessions.getIfPresent(sessionId);
    return session == null ? null : session.get(key);
  }

  @Override
  public void put(String sessionId, String key, Object value) {
    if (value == null) {
      Map<String, Object> session = sessions.getIfPresent(sessionId);
      if (session != null) {
        session.remove(key);
      }
      return;
    }
    Map<String, Object> session = sessions.getIfPresent(sessionId);
    if (session == null) {
      session = new ConcurrentHashMap<>();
      sessions.put(sessionId, session);
    }
    session.put(key, value);
  }

  @Override
  public void remove(String sessionId) {
    sessions.invalidate(sessionId);
  }
}
