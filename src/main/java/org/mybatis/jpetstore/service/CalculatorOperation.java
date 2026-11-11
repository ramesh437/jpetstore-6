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
package org.mybatis.jpetstore.service;

import java.util.Locale;

/**
 * Supported calculator operations.
 */
public enum CalculatorOperation {
  ADD, SUBTRACT, MULTIPLY, DIVIDE;

  /**
   * Parses an operation name case-insensitively.
   *
   * @param value
   *          the operation name
   *
   * @return the operation
   *
   * @throws IllegalArgumentException
   *           if the operation is unknown
   */
  public static CalculatorOperation parse(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Unsupported operation: null");
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unsupported operation: " + value, e);
    }
  }
}
