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

import java.io.Serializable;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Session state store backed by Google Cloud Memorystore for Redis. Only an opaque session id is kept in a cookie; all
 * state lives in a Redis hash (one hash per session) with a sliding expiration.
 */
public class RedisSessionStateStore implements SessionStateStore {

  /** The name of the cookie carrying the opaque session id. */
  public static final String COOKIE_NAME = "JPETSTORE_SID";

  private static final String SID_REQUEST_ATTRIBUTE = RedisSessionStateStore.class.getName() + ".SID";

  private final RedisTemplate<String, Object> redisTemplate;
  private final HashOperations<String, String, Object> hashOps;
  private final String keyPrefix;
  private final Duration ttl;

  /**
   * Instantiates a new redis session state store.
   *
   * @param connectionFactory
   *          the redis connection factory
   * @param keyPrefix
   *          the prefix for redis keys
   * @param ttl
   *          the sliding session timeout
   */
  public RedisSessionStateStore(RedisConnectionFactory connectionFactory, String keyPrefix, Duration ttl) {
    this.keyPrefix = keyPrefix;
    this.ttl = ttl;
    this.redisTemplate = new RedisTemplate<>();
    this.redisTemplate.setConnectionFactory(connectionFactory);
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    this.redisTemplate.setKeySerializer(stringSerializer);
    this.redisTemplate.setHashKeySerializer(stringSerializer);
    JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer(
        getClass().getClassLoader());
    this.redisTemplate.setValueSerializer(valueSerializer);
    this.redisTemplate.setHashValueSerializer(valueSerializer);
    this.redisTemplate.afterPropertiesSet();
    this.hashOps = this.redisTemplate.opsForHash();
  }

  @Override
  public <T> T get(String key, Class<T> type) {
    String sid = resolveSessionId(false);
    if (sid == null) {
      return null;
    }
    String redisKey = redisKey(sid);
    Object value = hashOps.get(redisKey, key);
    if (value == null) {
      return null;
    }
    redisTemplate.expire(redisKey, ttl);
    return type.isInstance(value) ? type.cast(value) : null;
  }

  @Override
  public void put(String key, Serializable value) {
    if (value == null) {
      remove(key);
      return;
    }
    String sid = resolveSessionId(true);
    if (sid == null) {
      throw new IllegalStateException("No active web request: cannot determine session id");
    }
    String redisKey = redisKey(sid);
    hashOps.put(redisKey, key, value);
    redisTemplate.expire(redisKey, ttl);
    HttpServletRequest request = currentRequest();
    if (request != null) {
      // make the state visible to views rendered within the same request
      request.setAttribute(key, value);
    }
  }

  @Override
  public void remove(String key) {
    String sid = resolveSessionId(false);
    if (sid != null) {
      hashOps.delete(redisKey(sid), key);
    }
    HttpServletRequest request = currentRequest();
    if (request != null) {
      request.removeAttribute(key);
    }
  }

  @Override
  public void invalidate() {
    String sid = resolveSessionId(false);
    HttpServletRequest request = currentRequest();
    if (sid != null) {
      String redisKey = redisKey(sid);
      Set<String> keys = hashOps.keys(redisKey);
      if (request != null && keys != null) {
        keys.forEach(request::removeAttribute);
      }
      redisTemplate.delete(redisKey);
    }
    if (request != null) {
      request.removeAttribute(SID_REQUEST_ATTRIBUTE);
    }
    HttpServletResponse response = currentResponse();
    if (response != null && sid != null) {
      Cookie cookie = newCookie(request, "");
      cookie.setMaxAge(0);
      response.addCookie(cookie);
    }
  }

  private String redisKey(String sid) {
    return keyPrefix + sid;
  }

  private String resolveSessionId(boolean create) {
    HttpServletRequest request = currentRequest();
    if (request == null) {
      return null;
    }
    Object existing = request.getAttribute(SID_REQUEST_ATTRIBUTE);
    if (existing != null) {
      return (String) existing;
    }
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (COOKIE_NAME.equals(cookie.getName()) && isValidSessionId(cookie.getValue())) {
          request.setAttribute(SID_REQUEST_ATTRIBUTE, cookie.getValue());
          return cookie.getValue();
        }
      }
    }
    if (!create) {
      return null;
    }
    String sid = UUID.randomUUID().toString();
    request.setAttribute(SID_REQUEST_ATTRIBUTE, sid);
    HttpServletResponse response = currentResponse();
    if (response != null) {
      response.addCookie(newCookie(request, sid));
    }
    return sid;
  }

  private static boolean isValidSessionId(String value) {
    if (value == null || value.length() != 36) {
      return false;
    }
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static Cookie newCookie(HttpServletRequest request, String value) {
    Cookie cookie = new Cookie(COOKIE_NAME, value);
    cookie.setHttpOnly(true);
    String contextPath = request == null ? "" : request.getContextPath();
    cookie.setPath(contextPath == null || contextPath.isEmpty() ? "/" : contextPath);
    cookie.setSecure(request != null
        && (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))));
    return cookie;
  }

  private static HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    return attributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) attributes).getRequest() : null;
  }

  private static HttpServletResponse currentResponse() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    return attributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) attributes).getResponse()
        : null;
  }
}
