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

import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.util.StringUtils;

/**
 * Factory helpers used by applicationContext.xml to wire the Google Cloud Memorystore for Redis backed HTTP session
 * store (Spring Session). All values are supplied from environment variables.
 */
public final class RedisSessionSupport {

  private RedisSessionSupport() {
    // static factory only
  }

  /**
   * Creates the Lettuce connection factory pointing at Memorystore for Redis.
   *
   * @param host
   *          the Redis host (Memorystore instance IP / hostname)
   * @param port
   *          the Redis port
   * @param password
   *          the Redis AUTH string (blank when AUTH is disabled)
   * @param ssl
   *          whether to use TLS (Memorystore in-transit encryption)
   *
   * @return the connection factory
   */
  public static LettuceConnectionFactory connectionFactory(String host, int port, String password, boolean ssl) {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
    if (StringUtils.hasText(password)) {
      configuration.setPassword(RedisPassword.of(password));
    }
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (ssl) {
      builder.useSsl();
    }
    return new LettuceConnectionFactory(configuration, builder.build());
  }

  /**
   * Memorystore does not allow the Redis CONFIG command, so skip keyspace-notification auto configuration.
   *
   * @return a no-op action
   */
  public static ConfigureRedisAction noOpConfigureRedisAction() {
    return ConfigureRedisAction.NO_OP;
  }
}
