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

/**
 * Externalised (stateless-application) store for per-user session state. Implementations must keep the state outside
 * of the application instance so that any instance can serve any request.
 */
public interface SessionStateStore {

  /**
   * Gets a session attribute.
   *
   * @param <T>
   *          the attribute type
   * @param sessionId
   *          the session id
   * @param key
   *          the attribute name
   * @param type
   *          the attribute type
   *
   * @return the attribute value or null if absent
   */
  <T> T get(String sessionId, String key, Class<T> type);

  /**
   * Stores a session attribute.
   *
   * @param sessionId
   *          the session id
   * @param key
   *          the attribute name
   * @param value
   *          the attribute value
   */
  void put(String sessionId, String key, Serializable value);

  /**
   * Removes all state of the given session.
   *
   * @param sessionId
   *          the session id
   */
  void invalidate(String sessionId);
}
