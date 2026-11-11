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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.jpetstore.service.CalculatorService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CalculatorControllerTest {

  private MockMvc mockMvc;

  private static ResultMatcher body(String fragment) {
    return result -> assertThat(result.getResponse().getContentAsString()).contains(fragment);
  }

  @BeforeEach
  void setUp() {
    CalculatorController controller = new CalculatorController();
    ReflectionTestUtils.setField(controller, "calculatorService", new CalculatorService());
    mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new CalculatorExceptionHandler()).build();
  }

  @Test
  void add() throws Exception {
    mockMvc.perform(get("/api/calculator/add").param("a", "1.5").param("b", "2")).andExpect(status().isOk())
        .andExpect(body("\"operation\":\"add\"")).andExpect(body("\"a\":1.5"))
        .andExpect(body("\"b\":2")).andExpect(body("\"result\":3.5"));
  }

  @Test
  void subtract() throws Exception {
    mockMvc.perform(get("/api/calculator/subtract").param("a", "1").param("b", "3")).andExpect(status().isOk())
        .andExpect(body("\"result\":-2"));
  }

  @Test
  void multiplyCaseInsensitive() throws Exception {
    mockMvc.perform(get("/api/calculator/MULTIPLY").param("a", "3").param("b", "4")).andExpect(status().isOk())
        .andExpect(body("\"operation\":\"multiply\"")).andExpect(body("\"result\":12"));
  }

  @Test
  void divide() throws Exception {
    mockMvc.perform(get("/api/calculator/divide").param("a", "10").param("b", "4")).andExpect(status().isOk())
        .andExpect(body("\"result\":2.5000000000"));
  }

  @Test
  void missingOperand() throws Exception {
    mockMvc.perform(get("/api/calculator/add").param("a", "1")).andExpect(status().isBadRequest())
        .andExpect(body("\"error\""));
  }

  @Test
  void nonNumericOperand() throws Exception {
    mockMvc.perform(get("/api/calculator/add").param("a", "x").param("b", "1")).andExpect(status().isBadRequest())
        .andExpect(body("\"error\""));
  }

  @Test
  void unknownOperation() throws Exception {
    mockMvc.perform(get("/api/calculator/pow").param("a", "1").param("b", "1")).andExpect(status().isBadRequest())
        .andExpect(body("\"error\""));
  }

  @Test
  void divideByZero() throws Exception {
    mockMvc.perform(get("/api/calculator/divide").param("a", "1").param("b", "0")).andExpect(status().isBadRequest())
        .andExpect(body("\"error\":\"Division by zero is not allowed\""));
  }
}
