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
package org.mybatis.jpetstore.web.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import javax.servlet.http.HttpSession;

import org.springframework.session.MapSession;
import org.springframework.session.Session;

/**
 * Test support that provides an {@link HttpSession} whose state lives in a Spring Session {@link Session} (the same
 * abstraction that is persisted to Memorystore for Redis in the deployed application) instead of in servlet container
 * memory. No server-local session state is used by the controller tests.
 */
final class SessionTestSupport {

  private SessionTestSupport() {
  }

  /**
   * Creates an HttpSession whose attributes are held by a Spring Session {@link MapSession}.
   *
   * @return the session-backed HttpSession
   */
  static HttpSession newSession() {
    final Session store = new MapSession();
    return mock(HttpSession.class, withSettings().defaultAnswer(invocation -> {
      switch (invocation.getMethod().getName()) {
        case "getAttribute":
          return store.getAttribute((String) invocation.getArgument(0));
        case "setAttribute":
          store.setAttribute((String) invocation.getArgument(0), invocation.getArgument(1));
          return null;
        case "removeAttribute":
          store.removeAttribute((String) invocation.getArgument(0));
          return null;
        case "getId":
          return store.getId();
        default:
          return null;
      }
    }));
  }
}
