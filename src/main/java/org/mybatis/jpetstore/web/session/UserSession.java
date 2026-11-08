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

/**
 * Externalized user session. Session state is not held in the servlet container's HTTP session but in a shared store
 * (Google Cloud Memorystore for Redis), so that any application instance can serve any request.
 */
public interface UserSession {

  /**
   * Gets an attribute.
   *
   * @param name
   *          the attribute name
   *
   * @return the value or null
   */
  Object getAttribute(String name);

  /**
   * Sets an attribute. Because values are serialized into the shared store, a mutated value must be written back with
   * this method to be persisted.
   *
   * @param name
   *          the attribute name
   * @param value
   *          the value
   */
  void setAttribute(String name, Object value);

  /**
   * Removes all state of this session.
   */
  void invalidate();
}
