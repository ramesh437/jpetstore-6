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
package org.mybatis.jpetstore.web.configuration;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that is true when a Memorystore for Redis endpoint is configured through the REDIS_HOST environment
 * variable (or the redis.host system property).
 */
public class RedisHostConfigured implements Condition {

  /**
   * Resolve the configured Redis host.
   *
   * @return the host or null when not configured
   */
  static String redisHost() {
    String host = System.getenv("REDIS_HOST");
    if (host == null || host.trim().isEmpty()) {
      host = System.getProperty("redis.host");
    }
    return host == null || host.trim().isEmpty() ? null : host.trim();
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return redisHost() != null;
  }
}
