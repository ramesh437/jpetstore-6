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

/**
 * Externalized (stateless-application) replacement for the container managed HTTP session.
 *
 * All user state (account, cart, order in progress) is kept in an external store (Google Cloud Memorystore for Redis)
 * so that any application instance can serve any request. API mirrors the subset of
 * {@code javax.servlet.http.HttpSession} that the application used.
 */
public interface SessionStateStore {

  /**
   * Gets the attribute for the current client, or {@code null}.
   *
   * @param name
   *          the attribute name
   *
   * @return the attribute value
   */
  Object getAttribute(String name);

  /**
   * Stores an attribute (must be {@link java.io.Serializable}) for the current client.
   *
   * @param name
   *          the attribute name
   * @param value
   *          the value
   */
  void setAttribute(String name, Object value);

  /**
   * Removes all state of the current client.
   */
  void invalidate();
}
