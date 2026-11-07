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

import static org.mockito.Mockito.spy;

import javax.servlet.http.HttpSession;

import org.springframework.mock.web.MockHttpSession;

/**
 * Test support that provides an {@link HttpSession} behaving like the externalized (Memorystore for Redis backed,
 * Spring Session) session used at runtime: attributes are held in a session store abstraction rather than in a
 * per-test Mockito mock of the container session. The returned session is a spy so that interactions can still be
 * verified and attributes can still be stubbed.
 */
final class SessionTestSupport {

  private SessionTestSupport() {
  }

  /**
   * Creates a new session whose state lives in a session store (no server affinity).
   *
   * @return the session
   */
  static HttpSession newSession() {
    return spy(new MockHttpSession());
  }
}
