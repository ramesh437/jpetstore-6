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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * {@link SessionStore} backed by Google Cloud Memorystore for Redis. Configuration is read from environment variables
 * (12-factor): REDIS_HOST, REDIS_PORT, REDIS_PASSWORD (e.g. injected from Secret Manager), REDIS_TLS and
 * SESSION_TTL_SECONDS.
 */
@Component
public class RedisSessionStore implements SessionStore, DisposableBean {

  private static final String KEY_PREFIX = "jpetstore:session:";
  private static final String ALLOWED_CLASSES = "org.mybatis.jpetstore.**;java.**;!*";

  private final String host = env("REDIS_HOST", "localhost");
  private final int port = Integer.parseInt(env("REDIS_PORT", "6379"));
  private final String password = env("REDIS_PASSWORD", "");
  private final boolean tls = Boolean.parseBoolean(env("REDIS_TLS", "false"));
  private final int ttlSeconds = Integer.parseInt(env("SESSION_TTL_SECONDS", "1800"));

  private volatile JedisPool pool;

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  private JedisPool pool() {
    JedisPool p = pool;
    if (p == null) {
      synchronized (this) {
        p = pool;
        if (p == null) {
          DefaultJedisClientConfig.Builder cfg = DefaultJedisClientConfig.builder().ssl(tls).timeoutMillis(2000);
          if (!password.isEmpty()) {
            cfg.password(password);
          }
          p = new JedisPool(new JedisPoolConfig(), new HostAndPort(host, port), cfg.build());
          pool = p;
        }
      }
    }
    return p;
  }

  private String redisKey() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    Object id = attributes == null ? null
        : attributes.getAttribute(SessionIdFilter.SESSION_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (id == null) {
      throw new IllegalStateException("No session id available for the current request");
    }
    return KEY_PREFIX + id;
  }

  @Override
  public Object get(String key) {
    String redisKey = redisKey();
    try (Jedis jedis = pool().getResource()) {
      byte[] data = jedis.hget(redisKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
      if (data == null) {
        return null;
      }
      jedis.expire(redisKey, ttlSeconds);
      return deserialize(data);
    }
  }

  @Override
  public void set(String key, Serializable value) {
    if (value == null) {
      remove(key);
      return;
    }
    byte[] redisKey = redisKey().getBytes(StandardCharsets.UTF_8);
    try (Jedis jedis = pool().getResource()) {
      jedis.hset(redisKey, key.getBytes(StandardCharsets.UTF_8), serialize(value));
      jedis.expire(redisKey, ttlSeconds);
    }
  }

  @Override
  public void remove(String key) {
    try (Jedis jedis = pool().getResource()) {
      jedis.hdel(redisKey().getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Override
  public void invalidate() {
    try (Jedis jedis = pool().getResource()) {
      jedis.del(redisKey());
    }
  }

  private static byte[] serialize(Serializable value) {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(value);
      out.flush();
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to serialize session attribute", e);
    }
  }

  private static Object deserialize(byte[] data) {
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
      in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(ALLOWED_CLASSES));
      return in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Unable to deserialize session attribute", e);
    }
  }

  @Override
  public void destroy() {
    JedisPool p = pool;
    if (p != null) {
      p.close();
    }
  }
}
