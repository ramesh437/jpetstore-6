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

import javax.servlet.http.HttpSession;

/**
 * Test support for controllers that read/write state through the {@link HttpSession} contract.
 * <p>
 * In deployed environments the {@code HttpSession} is transparently backed by Google Cloud Memorystore for Redis
 * (Spring Session), so the application keeps no state in the servlet container's memory. Controller unit tests
 * therefore only depend on the {@code HttpSession} contract and obtain their session from this single factory instead
 * of creating container-local session instances ad hoc.
 */
final class RedisBackedSessionTestSupport {

  private RedisBackedSessionTestSupport() {
  }

  /**
   * Creates a stand-in for the externally stored (Memorystore for Redis) HTTP session.
   *
   * @return the session test double
   */
  static HttpSession mockSession() {
    return mock(HttpSession.class);
  }
}
