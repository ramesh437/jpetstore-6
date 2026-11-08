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
 * {@link UserSession} whose state lives in a {@link SessionStateStore}.
 */
public class StoreBackedUserSession implements UserSession {

  /** The session id. */
  private final String sessionId;
  /** The store. */
  private final SessionStateStore store;
  /** The callback invoked on invalidation (expires the session cookie). */
  private final Runnable onInvalidate;

  /**
   * Instantiates a new store backed user session.
   *
   * @param sessionId
   *          the session id
   * @param store
   *          the store
   * @param onInvalidate
   *          the on invalidate callback
   */
  public StoreBackedUserSession(String sessionId, SessionStateStore store, Runnable onInvalidate) {
    this.sessionId = sessionId;
    this.store = store;
    this.onInvalidate = onInvalidate;
  }

  @Override
  public Object getAttribute(String name) {
    return store.get(sessionId, name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    store.put(sessionId, name, value);
  }

  @Override
  public void invalidate() {
    store.remove(sessionId);
    onInvalidate.run();
  }
}
