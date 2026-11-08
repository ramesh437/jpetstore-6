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

/**
 * Per-request handle to the externalized (Redis-backed) user session state. It replaces direct usage of the servlet
 * container's in-memory HttpSession so the application instances stay stateless.
 */
public class RedisSessionState {

  /** The session id. */
  private final String sessionId;
  /** The store. */
  private final RedisSessionStore store;

  /**
   * Instantiates a new redis session state.
   *
   * @param sessionId
   *          the session id
   * @param store
   *          the store
   */
  public RedisSessionState(String sessionId, RedisSessionStore store) {
    this.sessionId = sessionId;
    this.store = store;
  }

  /**
   * Gets the session id.
   *
   * @return the session id
   */
  public String getSessionId() {
    return sessionId;
  }

  /**
   * Gets the attribute.
   *
   * @param name
   *          the name
   *
   * @return the attribute
   */
  public Object getAttribute(String name) {
    return store.getAttribute(sessionId, name);
  }

  /**
   * Sets the attribute. Because state lives outside the JVM, mutated objects must be written back with this method.
   *
   * @param name
   *          the name
   * @param value
   *          the value
   */
  public void setAttribute(String name, Object value) {
    store.setAttribute(sessionId, name, value);
  }

  /**
   * Removes the attribute.
   *
   * @param name
   *          the name
   */
  public void removeAttribute(String name) {
    store.removeAttribute(sessionId, name);
  }

  /**
   * Invalidate the session.
   */
  public void invalidate() {
    store.invalidate(sessionId);
  }
}
