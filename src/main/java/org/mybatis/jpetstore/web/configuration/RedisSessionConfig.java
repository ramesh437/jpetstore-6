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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Externalizes HTTP session state to Google Cloud Memorystore for Redis using Spring Session. The application instances
 * become stateless; the session (account, cart, order) is stored centrally in Redis.
 * <p>
 * Configuration (12-factor, environment variables):
 * <ul>
 * <li>REDIS_HOST - Memorystore instance IP/host (default: localhost)</li>
 * <li>REDIS_PORT - Memorystore port (default: 6379)</li>
 * <li>REDIS_PASSWORD - AUTH string, optional (inject from Secret Manager)</li>
 * </ul>
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800, redisNamespace = "jpetstore:session")
public class RedisSessionConfig {

  /**
   * Redis connection factory pointing at Memorystore for Redis.
   *
   * @return the connection factory
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    String host = getEnv("REDIS_HOST", "localhost");
    int port = Integer.parseInt(getEnv("REDIS_PORT", "6379"));
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
    String password = System.getenv("REDIS_PASSWORD");
    if (password != null && !password.isEmpty()) {
      configuration.setPassword(RedisPassword.of(password));
    }
    return new LettuceConnectionFactory(configuration);
  }

  private static String getEnv(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? defaultValue : value;
  }
}
