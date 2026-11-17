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

import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Base class that identifies a client by an opaque session-id cookie. The state itself lives in the external store
 * implemented by subclasses; nothing is kept in the servlet container.
 */
public abstract class AbstractCookieSessionStateStore implements SessionStateStore {

  /** The name of the cookie carrying the external session id. */
  public static final String COOKIE_NAME = "JPETSTORE_SID";

  private static final String REQUEST_ATTRIBUTE = AbstractCookieSessionStateStore.class.getName() + ".SID";

  private static final Pattern SID_PATTERN = Pattern
      .compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

  /**
   * Load a value.
   *
   * @param sid
   *          the session id
   * @param name
   *          the attribute name
   *
   * @return the value or null
   */
  protected abstract Object load(String sid, String name);

  /**
   * Store a value.
   *
   * @param sid
   *          the session id
   * @param name
   *          the attribute name
   * @param value
   *          the value
   */
  protected abstract void store(String sid, String name, Object value);

  /**
   * Delete all values of a session.
   *
   * @param sid
   *          the session id
   */
  protected abstract void delete(String sid);

  @Override
  public Object getAttribute(String name) {
    String sid = currentSessionId(false);
    return sid == null ? null : load(sid, name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    String sid = currentSessionId(true);
    store(sid, name, value);
  }

  @Override
  public void invalidate() {
    String sid = currentSessionId(false);
    if (sid == null) {
      return;
    }
    delete(sid);
    ServletRequestAttributes attrs = currentAttributes();
    if (attrs != null) {
      attrs.removeAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
      HttpServletResponse response = attrs.getResponse();
      if (response != null) {
        response.addHeader("Set-Cookie", buildCookie(attrs.getRequest(), "", 0));
      }
    }
  }

  private ServletRequestAttributes currentAttributes() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    return attrs instanceof ServletRequestAttributes ? (ServletRequestAttributes) attrs : null;
  }

  private String currentSessionId(boolean create) {
    ServletRequestAttributes attrs = currentAttributes();
    if (attrs == null) {
      if (create) {
        throw new IllegalStateException("No current web request; cannot resolve session state");
      }
      return null;
    }
    String sid = (String) attrs.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (sid != null) {
      return sid;
    }
    HttpServletRequest request = attrs.getRequest();
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
            && SID_PATTERN.matcher(cookie.getValue()).matches()) {
          sid = cookie.getValue();
          break;
        }
      }
    }
    if (sid == null && create) {
      sid = UUID.randomUUID().toString();
      HttpServletResponse response = attrs.getResponse();
      if (response != null) {
        response.addHeader("Set-Cookie", buildCookie(request, sid, -1));
      }
    }
    if (sid != null) {
      attrs.setAttribute(REQUEST_ATTRIBUTE, sid, RequestAttributes.SCOPE_REQUEST);
    }
    return sid;
  }

  private String buildCookie(HttpServletRequest request, String value, int maxAge) {
    String path = request.getContextPath();
    if (path == null || path.isEmpty()) {
      path = "/";
    }
    StringBuilder sb = new StringBuilder(COOKIE_NAME).append('=').append(value).append("; Path=").append(path)
        .append("; HttpOnly; SameSite=Lax");
    if (maxAge >= 0) {
      sb.append("; Max-Age=").append(maxAge);
    }
    if (request.isSecure()) {
      sb.append("; Secure");
    }
    return sb.toString();
  }
}
