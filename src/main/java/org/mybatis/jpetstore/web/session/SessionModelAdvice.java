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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the signed-on account (kept in the external session store) to the views as <code>accountBean</code>,
 * replacing the former servlet <code>sessionScope.accountBean</code>.
 */
@ControllerAdvice
public class SessionModelAdvice {

  /** The factory. */
  @Autowired
  private UserSessionFactory userSessionFactory;

  /**
   * Account bean.
   *
   * @param request
   *          the request
   *
   * @return the account session object or null if not signed on
   */
  @ModelAttribute("accountBean")
  public Object accountBean(HttpServletRequest request) {
    UserSession session = userSessionFactory.find(request);
    return session == null ? null : session.getAttribute("accountBean");
  }
}
