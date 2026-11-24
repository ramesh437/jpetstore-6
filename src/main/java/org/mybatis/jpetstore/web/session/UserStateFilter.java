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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Binds a Redis backed {@link UserState} to every request (request attribute {@code userState}). Registered in
 * web.xml through a DelegatingFilterProxy named {@code userStateFilter}.
 */
@Component("userStateFilter")
public class UserStateFilter implements Filter {

  @Autowired
  private SessionStateStore store;

  /** Optional override (env SESSION_COOKIE_SECURE) for the Secure cookie flag; empty = auto detect. */
  @Value("${SESSION_COOKIE_SECURE:}")
  private String secureOverride;

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String requestedId = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (UserState.COOKIE_NAME.equals(cookie.getName())) {
          requestedId = cookie.getValue();
          break;
        }
      }
    }
    boolean secure;
    if (secureOverride != null && !secureOverride.trim().isEmpty()) {
      secure = Boolean.parseBoolean(secureOverride.trim());
    } else {
      secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    request.setAttribute(UserState.REQUEST_ATTRIBUTE,
        new UserState(store, response, request.getContextPath(), secure, requestedId));
    chain.doFilter(request, response);
  }
}
