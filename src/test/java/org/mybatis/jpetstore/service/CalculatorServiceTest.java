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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CalculatorServiceTest {

  private final CalculatorService service = new CalculatorService();

  private static BigDecimal d(String s) {
    return new BigDecimal(s);
  }

  @Test
  void add() {
    assertThat(service.calculate(CalculatorOperation.ADD, d("0.1"), d("0.2"))).isEqualByComparingTo("0.3");
  }

  @Test
  void subtractNegatives() {
    assertThat(service.calculate(CalculatorOperation.SUBTRACT, d("-5"), d("-8"))).isEqualByComparingTo("3");
  }

  @Test
  void multiply() {
    assertThat(service.calculate(CalculatorOperation.MULTIPLY, d("1.5"), d("-2"))).isEqualByComparingTo("-3");
  }

  @Test
  void divide() {
    assertThat(service.calculate(CalculatorOperation.DIVIDE, d("10"), d("4"))).isEqualByComparingTo("2.5");
  }

  @Test
  void divideNonTerminatingUsesScale() {
    BigDecimal r = service.calculate(CalculatorOperation.DIVIDE, d("1"), d("3"));
    assertThat(r).isEqualTo(d("0.3333333333"));
    assertThat(r.scale()).isEqualTo(10);
  }

  @Test
  void divideByZero() {
    assertThatThrownBy(() -> service.calculate(CalculatorOperation.DIVIDE, d("1"), d("0.0")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("zero");
  }

  @Test
  void parseCaseInsensitive() {
    assertThat(CalculatorOperation.parse("MuLtIpLy")).isEqualTo(CalculatorOperation.MULTIPLY);
    assertThatThrownBy(() -> CalculatorOperation.parse("pow")).isInstanceOf(IllegalArgumentException.class);
  }
}
