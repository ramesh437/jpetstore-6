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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local fallback used ONLY for local development / tests when no {@code REDIS_HOST} is configured. It is not
 * suitable for multi-instance deployments; in GCP always configure Memorystore for Redis.
 */
public class LocalSessionStateStore extends AbstractCookieSessionStateStore {

  private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

  @Override
  protected Object load(String sid, String name) {
    Map<String, Object> s = sessions.get(sid);
    return s == null ? null : s.get(name);
  }

  @Override
  protected void store(String sid, String name, Object value) {
    Map<String, Object> s = sessions.computeIfAbsent(sid, k -> new ConcurrentHashMap<>());
    if (value == null) {
      s.remove(name);
    } else {
      s.put(name, value);
    }
  }

  @Override
  protected void delete(String sid) {
    sessions.remove(sid);
  }
}
