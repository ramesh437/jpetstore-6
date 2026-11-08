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

import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Resolves (or creates) the session id cookie, exposes the Redis-backed session state to controllers and makes the
 * signed-on account available to JSP views as the request attribute {@code accountBean}.
 */
public class RedisSessionInterceptor implements HandlerInterceptor {

  /** The session cookie name. */
  public static final String COOKIE_NAME = "JPETSTORE_SID";
  /** The request attribute holding the {@link RedisSessionState}. */
  public static final String STATE_ATTRIBUTE = RedisSessionInterceptor.class.getName() + ".STATE";

  /** The store. */
  private final RedisSessionStore store;
  /** The cookie max age in seconds. */
  private final int cookieMaxAge;

  /**
   * Instantiates a new redis session interceptor.
   *
   * @param store
   *          the store
   * @param cookieMaxAge
   *          the cookie max age in seconds
   */
  public RedisSessionInterceptor(RedisSessionStore store, int cookieMaxAge) {
    this.store = store;
    this.cookieMaxAge = cookieMaxAge;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String sessionId = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (COOKIE_NAME.equals(cookie.getName()) && isValid(cookie.getValue())) {
          sessionId = cookie.getValue();
          break;
        }
      }
    }
    if (sessionId == null) {
      sessionId = UUID.randomUUID().toString();
      boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
      response.addHeader("Set-Cookie", COOKIE_NAME + "=" + sessionId + "; Path=/; Max-Age=" + cookieMaxAge
          + "; HttpOnly; SameSite=Lax" + (secure ? "; Secure" : ""));
    }
    request.setAttribute(STATE_ATTRIBUTE, new RedisSessionState(sessionId, store));
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
    if (modelAndView == null || modelAndView.getViewName() == null
        || modelAndView.getViewName().startsWith("redirect:")) {
      return;
    }
    RedisSessionState state = (RedisSessionState) request.getAttribute(STATE_ATTRIBUTE);
    if (state != null) {
      Object accountBean = state.getAttribute("accountBean");
      if (accountBean != null) {
        request.setAttribute("accountBean", accountBean);
      }
    }
  }

  private static boolean isValid(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
