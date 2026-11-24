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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

/**
 * Per-request handle to the user's externalised (Redis) state. It replaces the container managed HttpSession so that
 * the application instances stay stateless.
 * <p>
 * Values are written through to Redis immediately. Because state lives outside the JVM, a mutated object (for example
 * the cart) must be stored again with {@link #setAttribute(String, Object)} to be persisted.
 */
public class UserState {

  /** The request attribute under which the state handle is exposed to controllers and JSPs. */
  public static final String REQUEST_ATTRIBUTE = "userState";
  /** The name of the state id cookie. */
  public static final String COOKIE_NAME = "JPETSTORE_SID";

  private static final Pattern VALID_ID = Pattern.compile("[0-9a-f]{32}");

  private final SessionStateStore store;
  private final HttpServletResponse response;
  private final String cookiePath;
  private final boolean secure;
  private final Map<String, Object> cache = new HashMap<>();
  private String id;
  private boolean touched;

  /**
   * Instantiates a new user state.
   *
   * @param store
   *          the store
   * @param response
   *          the response used to issue the state cookie
   * @param cookiePath
   *          the cookie path
   * @param secure
   *          whether the cookie must carry the Secure flag
   * @param requestedId
   *          the id received in the cookie (may be null or invalid)
   */
  public UserState(SessionStateStore store, HttpServletResponse response, String cookiePath, boolean secure,
      String requestedId) {
    this.store = store;
    this.response = response;
    this.cookiePath = cookiePath == null || cookiePath.isEmpty() ? "/" : cookiePath;
    this.secure = secure;
    this.id = requestedId != null && VALID_ID.matcher(requestedId).matches() ? requestedId : null;
  }

  /**
   * Gets an attribute.
   *
   * @param name
   *          the name
   *
   * @return the attribute or null
   */
  public Object getAttribute(String name) {
    if (id == null) {
      return null;
    }
    if (cache.containsKey(name)) {
      return cache.get(name);
    }
    if (!touched) {
      store.touch(id);
      touched = true;
    }
    Object value = store.get(id, name);
    cache.put(name, value);
    return value;
  }

  /**
   * Sets (persists) an attribute. A null value removes the attribute.
   *
   * @param name
   *          the name
   * @param value
   *          the value
   */
  public void setAttribute(String name, Object value) {
    if (value == null) {
      removeAttribute(name);
      return;
    }
    if (id == null) {
      id = newId();
      writeCookie(id, -1);
    }
    store.put(id, name, value);
    cache.put(name, value);
  }

  /**
   * Removes an attribute.
   *
   * @param name
   *          the name
   */
  public void removeAttribute(String name) {
    if (id != null) {
      store.remove(id, name);
    }
    cache.remove(name);
  }

  /**
   * Invalidates all state of the user and expires the cookie.
   */
  public void invalidate() {
    if (id != null) {
      store.delete(id);
      writeCookie("", 0);
    }
    id = null;
    cache.clear();
  }

  /**
   * Issues a new state id keeping the data (call after authentication to prevent session fixation).
   */
  public void rotateId() {
    if (id == null) {
      return;
    }
    String newId = newId();
    store.rename(id, newId);
    id = newId;
    writeCookie(id, -1);
  }

  /**
   * Gets the signed on account bean, used by the JSPs as ${userState.accountBean}.
   *
   * @return the account bean or null
   */
  public Object getAccountBean() {
    return getAttribute("accountBean");
  }

  private static String newId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private void writeCookie(String value, int maxAge) {
    StringBuilder cookie = new StringBuilder(COOKIE_NAME).append('=').append(value).append("; Path=")
        .append(cookiePath).append("; HttpOnly; SameSite=Lax");
    if (maxAge >= 0) {
      cookie.append("; Max-Age=").append(maxAge);
    }
    if (secure) {
      cookie.append("; Secure");
    }
    response.addHeader("Set-Cookie", cookie.toString());
  }
}
