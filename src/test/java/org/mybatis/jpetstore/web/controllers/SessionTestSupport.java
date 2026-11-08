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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

/**
 * Test support that simulates the externalised (Spring Session / Google Cloud Memorystore for Redis) HTTP session. All
 * session attributes are kept in an external key/value store instead of in the state of the test or application
 * instance, mirroring how the application keeps no state in local memory in production.
 */
final class SessionTestSupport {

  private SessionTestSupport() {
  }

  /**
   * Creates an {@link HttpSession} whose attributes live in an in-memory stand-in for the Redis session store.
   *
   * @return the externalised session test double
   */
  static HttpSession newExternalizedSession() {
    final Map<String, Object> redisStore = new ConcurrentHashMap<>();
    return mock(HttpSession.class, withSettings().defaultAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      switch (invocation.getMethod().getName()) {
        case "getAttribute":
          return redisStore.get((String) args[0]);
        case "setAttribute":
          if (args[1] == null) {
            redisStore.remove((String) args[0]);
          } else {
            redisStore.put((String) args[0], args[1]);
          }
          return null;
        case "removeAttribute":
          redisStore.remove((String) args[0]);
          return null;
        case "invalidate":
          redisStore.clear();
          return null;
        default:
          return null;
      }
    }));
  }
}
