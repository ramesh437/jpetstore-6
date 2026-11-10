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
 * Test support that provides a session whose attributes live in an external (store-like) container rather than in a
 * hand-stubbed servlet container session. In production the HTTP session is externalized to Google Cloud Memorystore
 * for Redis through Spring Session (see {@code RedisSessionConfiguration}); attributes are only persisted when they are
 * explicitly set. This helper mirrors those semantics in an in-memory {@link MockHttpSession} (wrapped in a spy so
 * interactions can still be verified).
 */
final class SessionTestSupport {

  private SessionTestSupport() {
  }

  /**
   * Creates a new in-memory session that emulates the Redis-backed (Spring Session) HTTP session.
   *
   * @return the session
   */
  static HttpSession newSession() {
    return spy(new MockHttpSession());
  }
}
