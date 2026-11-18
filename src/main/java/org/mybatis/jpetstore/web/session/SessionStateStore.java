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

import java.io.Serializable;

/**
 * Externalized (stateless application tier) session state store. Implementations keep per-user state outside of the
 * application instance so that any instance behind a load balancer can serve any request.
 */
public interface SessionStateStore {

  /**
   * Gets a value of the current user's session state.
   *
   * @param <T>
   *          the value type
   * @param key
   *          the attribute key
   * @param type
   *          the expected type
   *
   * @return the value or {@code null} if absent
   */
  <T> T get(String key, Class<T> type);

  /**
   * Stores a value in the current user's session state.
   *
   * @param key
   *          the attribute key
   * @param value
   *          the value
   */
  void put(String key, Serializable value);

  /**
   * Removes a value from the current user's session state.
   *
   * @param key
   *          the attribute key
   */
  void remove(String key);

  /**
   * Destroys the complete session state of the current user.
   */
  void invalidate();
}
