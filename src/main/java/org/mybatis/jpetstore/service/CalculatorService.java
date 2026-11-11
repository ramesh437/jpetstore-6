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

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

/**
 * Stateless two-number arithmetic service.
 */
@Service
public class CalculatorService {

  private static final int DIVIDE_SCALE = 10;

  /**
   * Calculate.
   *
   * @param operation
   *          the operation
   * @param a
   *          first operand
   * @param b
   *          second operand
   *
   * @return the result
   */
  public BigDecimal calculate(CalculatorOperation operation, BigDecimal a, BigDecimal b) {
    switch (operation) {
      case ADD:
        return a.add(b);
      case SUBTRACT:
        return a.subtract(b);
      case MULTIPLY:
        return a.multiply(b);
      case DIVIDE:
        if (b.signum() == 0) {
          throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return a.divide(b, DIVIDE_SCALE, RoundingMode.HALF_UP);
      default:
        throw new IllegalArgumentException("Unsupported operation: " + operation);
    }
  }
}
