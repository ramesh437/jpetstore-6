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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Exposes the signed-on account state (loaded from the external session store) to the JSP views as the request
 * attribute {@code accountBean}, replacing the former {@code sessionScope.accountBean} lookup.
 */
@Component
public class SessionModelInterceptor implements HandlerInterceptor {

  /** The session store. */
  @Autowired
  private SessionStore sessionStore;

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
    if (modelAndView != null && modelAndView.hasView()) {
      String viewName = modelAndView.getViewName();
      if (viewName == null || !viewName.startsWith("redirect:")) {
        request.setAttribute("accountBean", sessionStore.get("accountBean"));
      }
    }
  }
}
