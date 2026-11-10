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
 * Externalized (stateless application tier) store for per-user session state. Implementations keep the state outside
 * of the application instance (e.g. Google Cloud Memorystore for Redis) so that any instance can serve any request.
 */
public interface SessionStore {

  /**
   * Gets the value stored under the given key for the current user.
   *
   * @param key
   *          the key
   *
   * @return the value or {@code null} if absent
   */
  Object get(String key);

  /**
   * Stores a value under the given key for the current user. Callers must re-store mutated objects.
   *
   * @param key
   *          the key
   * @param value
   *          the value
   */
  void set(String key, Serializable value);

  /**
   * Removes the value stored under the given key.
   *
   * @param key
   *          the key
   */
  void remove(String key);

  /**
   * Removes all state of the current user.
   */
  void invalidate();
}
