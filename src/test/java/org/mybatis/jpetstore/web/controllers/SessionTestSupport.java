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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpSession;

import org.springframework.session.MapSession;
import org.springframework.session.Session;

/**
 * Test support that provides a servlet session view backed by a Spring Session {@link Session}.
 * <p>
 * In deployed environments HTTP session state is externalized to Memorystore for Redis through Spring Session (see
 * {@code RedisSessionConfig}). Tests therefore exercise the controllers against a Spring Session model
 * ({@link MapSession}, the same representation that is persisted to Redis) instead of container-managed in-memory
 * HttpSession state. No Redis server is required for unit tests.
 */
final class SessionTestSupport {

  private SessionTestSupport() {
  }

  /**
   * Creates a servlet session whose attributes are stored in a Spring Session {@link MapSession}.
   *
   * @return the session view
   */
  static HttpSession newSession() {
    final Session store = new MapSession();
    HttpSession view = mock(HttpSession.class);
    lenient().when(view.getId()).thenReturn(store.getId());
    lenient().when(view.getAttribute(anyString())).thenAnswer(inv -> store.getAttribute(inv.getArgument(0)));
    lenient().doAnswer(inv -> {
      store.setAttribute(inv.getArgument(0), inv.getArgument(1));
      return null;
    }).when(view).setAttribute(anyString(), any());
    lenient().doAnswer(inv -> {
      store.removeAttribute(inv.getArgument(0));
      return null;
    }).when(view).removeAttribute(anyString());
    return view;
  }
}
