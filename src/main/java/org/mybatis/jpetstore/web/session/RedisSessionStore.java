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
import java.nio.charset.StandardCharsets;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Externalized session state store backed by Google Cloud Memorystore for Redis. Each user session is stored as a Redis
 * hash (one field per attribute) with a sliding time-to-live, so that no application instance holds user state in
 * memory and any instance can serve any request.
 */
public class RedisSessionStore {

  /** Allow-list used when deserializing session values. */
  private static final ObjectInputFilter FILTER = ObjectInputFilter.Config
      .createFilter("org.mybatis.jpetstore.**;java.lang.**;java.util.**;java.math.**;java.time.**;!*");

  /** The Redis connection pool. */
  private final JedisPool pool;
  /** The session time-to-live in seconds. */
  private final int ttlSeconds;
  /** The Redis key prefix. */
  private final String keyPrefix;

  /**
   * Instantiates a new redis session store.
   *
   * @param host
   *          the Memorystore host
   * @param port
   *          the Memorystore port
   * @param password
   *          the AUTH string (may be blank)
   * @param ssl
   *          true to use TLS (in-transit encryption)
   * @param ttlSeconds
   *          the session time-to-live in seconds
   * @param keyPrefix
   *          the Redis key prefix
   */
  public RedisSessionStore(String host, int port, String password, boolean ssl, int ttlSeconds, String keyPrefix) {
    String auth = password == null || password.trim().isEmpty() ? null : password;
    this.pool = new JedisPool(new JedisPoolConfig(), host, port, 2000, auth, ssl);
    this.ttlSeconds = ttlSeconds;
    this.keyPrefix = keyPrefix;
  }

  /**
   * Gets the attribute.
   *
   * @param sessionId
   *          the session id
   * @param name
   *          the attribute name
   *
   * @return the attribute value or null
   */
  public Object getAttribute(String sessionId, String name) {
    byte[] key = key(sessionId);
    try (Jedis jedis = pool.getResource()) {
      byte[] value = jedis.hget(key, bytes(name));
      if (value == null) {
        return null;
      }
      jedis.expire(key, ttlSeconds);
      return deserialize(value);
    }
  }

  /**
   * Sets the attribute.
   *
   * @param sessionId
   *          the session id
   * @param name
   *          the attribute name
   * @param value
   *          the attribute value (must be Serializable); null removes the attribute
   */
  public void setAttribute(String sessionId, String name, Object value) {
    if (value == null) {
      removeAttribute(sessionId, name);
      return;
    }
    byte[] key = key(sessionId);
    byte[] data = serialize(value);
    try (Jedis jedis = pool.getResource()) {
      jedis.hset(key, bytes(name), data);
      jedis.expire(key, ttlSeconds);
    }
  }

  /**
   * Removes the attribute.
   *
   * @param sessionId
   *          the session id
   * @param name
   *          the attribute name
   */
  public void removeAttribute(String sessionId, String name) {
    try (Jedis jedis = pool.getResource()) {
      jedis.hdel(key(sessionId), bytes(name));
    }
  }

  /**
   * Invalidates (deletes) the whole session.
   *
   * @param sessionId
   *          the session id
   */
  public void invalidate(String sessionId) {
    try (Jedis jedis = pool.getResource()) {
      jedis.del(key(sessionId));
    }
  }

  /**
   * Close the connection pool.
   */
  public void close() {
    pool.close();
  }

  private byte[] key(String sessionId) {
    return bytes(keyPrefix + sessionId);
  }

  private static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] serialize(Object value) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(value);
      oos.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to serialize session attribute", e);
    }
  }

  private static Object deserialize(byte[] data) {
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
      ois.setObjectInputFilter(FILTER);
      return ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Unable to deserialize session attribute", e);
    }
  }
}
