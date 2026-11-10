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
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Assigns an opaque session identifier (cookie) to every client. The identifier is only a key into the external
 * session store (Memorystore for Redis); no state is kept in the application instance or in the servlet container.
 */
@WebFilter(urlPatterns = "/*")
public class SessionIdFilter implements Filter {

  /** Request attribute holding the session id. */
  public static final String SESSION_ID_ATTRIBUTE = "org.mybatis.jpetstore.SESSION_ID";
  /** Cookie name. */
  public static final String COOKIE_NAME = "JPETSTORE_SID";

  private static final Pattern VALID_ID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing to initialize
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String sessionId = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
            && VALID_ID.matcher(cookie.getValue()).matches()) {
          sessionId = cookie.getValue();
          break;
        }
      }
    }
    if (sessionId == null) {
      sessionId = UUID.randomUUID().toString();
      boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
      String path = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
      response.addHeader("Set-Cookie",
          COOKIE_NAME + "=" + sessionId + "; Path=" + path + "; HttpOnly; SameSite=Lax" + (secure ? "; Secure" : ""));
    }
    request.setAttribute(SESSION_ID_ATTRIBUTE, sessionId);
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // nothing to release
  }
}
