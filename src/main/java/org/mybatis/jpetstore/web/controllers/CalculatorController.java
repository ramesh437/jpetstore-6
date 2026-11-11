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
package org.mybatis.jpetstore.web.controllers;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mybatis.jpetstore.service.CalculatorOperation;
import org.mybatis.jpetstore.service.CalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public calculator REST API.
 */
@RestController
@RequestMapping("/api/calculator")
public class CalculatorController {

  @Autowired
  private CalculatorService calculatorService;

  /**
   * Calculate.
   *
   * @param operation
   *          add, subtract, multiply or divide
   * @param a
   *          first operand
   * @param b
   *          second operand
   *
   * @return the response body
   */
  @GetMapping(value = "/{operation}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> calculate(@PathVariable("operation") String operation,
      @RequestParam(name = "a", required = false) BigDecimal a,
      @RequestParam(name = "b", required = false) BigDecimal b) {
    CalculatorOperation op = CalculatorOperation.parse(operation);
    if (a == null || b == null) {
      throw new IllegalArgumentException("Parameters 'a' and 'b' are required");
    }
    BigDecimal result = calculatorService.calculate(op, a, b);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("operation", op.name().toLowerCase(java.util.Locale.ROOT));
    body.put("a", a);
    body.put("b", b);
    body.put("result", result);
    return body;
  }
}
