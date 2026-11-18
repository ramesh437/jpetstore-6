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

import org.mybatis.jpetstore.web.controllers.AccountController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Identifies the caller with an opaque session cookie and exposes the identifier ("sessionId") and the signed-on
 * account ("accountBean") as request attributes. All state lives in {@link SessionStateStore}; nothing is held in the
 * application server's HTTP session.
 */
@Component
public class SessionIdInterceptor implements HandlerInterceptor {

  /** Request attribute holding the session id. */
  public static final String SESSION_ID_ATTRIBUTE = "sessionId";
  /** Request attribute holding the account session (for JSPs). */
  public static final String ACCOUNT_ATTRIBUTE = "accountBean";

  /** Name of the session cookie. */
  private static final String COOKIE_NAME = "JPETSTORE_SID";
  /** Valid session id format. */
  private static final Pattern UUID_PATTERN = Pattern
      .compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

  /** The session state store. */
  @Autowired
  private SessionStateStore sessionStateStore;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String sessionId = readSessionId(request);
    if (sessionId == null) {
      sessionId = UUID.randomUUID().toString();
      response.addHeader("Set-Cookie", COOKIE_NAME + "=" + sessionId + "; Path="
          + (request.getContextPath().isEmpty() ? "/" : request.getContextPath()) + "; HttpOnly; SameSite=Lax"
          + (isSecure(request) ? "; Secure" : ""));
    }
    request.setAttribute(SESSION_ID_ATTRIBUTE, sessionId);
    request.setAttribute(ACCOUNT_ATTRIBUTE,
        sessionStateStore.get(sessionId, ACCOUNT_ATTRIBUTE, AccountController.AccountSession.class));
    return true;
  }

  private static String readSessionId(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
            && UUID_PATTERN.matcher(cookie.getValue()).matches()) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  private static boolean isSecure(HttpServletRequest request) {
    return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
  }
}
