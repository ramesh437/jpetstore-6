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

import java.security.SecureRandom;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link UserSession} of a request. The session id travels in a cookie; all state is kept in the
 * {@link SessionStateStore}.
 */
@Component
public class UserSessionFactory {

  /** The session cookie name. */
  public static final String COOKIE_NAME = "JPETSTORE_SID";
  /** Request attribute used to share the resolved session within a request. */
  private static final String REQUEST_ATTRIBUTE = UserSessionFactory.class.getName() + ".SESSION";
  /** Valid session id format (prevents arbitrary keys being supplied by clients). */
  private static final Pattern SESSION_ID_PATTERN = Pattern.compile("[0-9a-f]{64}");

  /** The secure random. */
  private final SecureRandom secureRandom = new SecureRandom();

  /** The store. */
  @Autowired
  private SessionStateStore store;

  /**
   * Finds the session of the request without creating one.
   *
   * @param request
   *          the request
   *
   * @return the session or null if the client has no (valid) session cookie
   */
  public UserSession find(HttpServletRequest request) {
    Object existing = request.getAttribute(REQUEST_ATTRIBUTE);
    if (existing instanceof UserSession) {
      return (UserSession) existing;
    }
    String sessionId = readSessionId(request);
    return sessionId == null ? null : new StoreBackedUserSession(sessionId, store, () -> {
    });
  }

  /**
   * Gets the session of the request, creating it (and issuing the cookie) when necessary.
   *
   * @param request
   *          the request
   * @param response
   *          the response
   *
   * @return the session
   */
  public UserSession getOrCreate(HttpServletRequest request, HttpServletResponse response) {
    Object existing = request.getAttribute(REQUEST_ATTRIBUTE);
    if (existing instanceof UserSession) {
      return (UserSession) existing;
    }
    String sessionId = readSessionId(request);
    if (sessionId == null) {
      sessionId = newSessionId();
      response.addHeader("Set-Cookie", cookieHeader(request, sessionId, -1));
    }
    return register(request,
        new StoreBackedUserSession(sessionId, store,
            () -> response.addHeader("Set-Cookie", cookieHeader(request, "", 0))));
  }

  private UserSession register(HttpServletRequest request, UserSession session) {
    request.setAttribute(REQUEST_ATTRIBUTE, session);
    return session;
  }

  private String readSessionId(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
          && SESSION_ID_PATTERN.matcher(cookie.getValue()).matches()) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private String newSessionId() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    StringBuilder sb = new StringBuilder(64);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  private String cookieHeader(HttpServletRequest request, String value, int maxAge) {
    String contextPath = request.getContextPath();
    StringBuilder sb = new StringBuilder(COOKIE_NAME).append('=').append(value).append("; Path=")
        .append(contextPath == null || contextPath.isEmpty() ? "/" : contextPath).append("; HttpOnly; SameSite=Lax");
    if (maxAge >= 0) {
      sb.append("; Max-Age=").append(maxAge);
    }
    if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
      sb.append("; Secure");
    }
    return sb.toString();
  }
}
