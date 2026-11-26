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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.service.OrderService;
import org.mybatis.jpetstore.session.RedisSessionService;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

/**
 * The Class OrderControllerTest.
 *
 * <p>Order and cart session state is backed by Google Cloud Memorystore for Redis via
 * {@link RedisSessionService}. Tests mock {@link RedisSessionService} instead of
 * relying on {@link HttpSession} attribute storage, reflecting the cloud-native
 * stateless architecture.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

  /** The order service. */
  @Mock
  private OrderService orderService;

  /**
   * Redis-backed session service mock — replaces direct HttpSession attribute
   * storage with Google Cloud Memorystore for Redis to support stateless
   * horizontal scaling.
   */
  @Mock
  private RedisSessionService redisSessionService;

  /** The order controller. */
  @InjectMocks
  private OrderController orderController;

  /**
   * List orders without auth returns error.
   */
  @Test
  void listOrdersWithoutAuthReturnsError() {
    // Use a mock HttpSession to supply a session ID; accountBean state is stored in Redis, not in the session itself
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();
    when(redisSessionService.getAttribute("test-session-id", "accountBean",
        AccountController.AccountSession.class)).thenReturn(null);

    String view = orderController.listOrders(session, model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsKey("message");
  }

  /**
   * New order form without auth redirects.
   */
  @Test
  void newOrderFormWithoutAuthRedirects() {
    // Use a mock HttpSession to supply a session ID; accountBean and cart state are stored in Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();
    when(redisSessionService.getAttribute("test-session-id", "accountBean",
        AccountController.AccountSession.class)).thenReturn(null);
    when(redisSessionService.getAttribute("test-session-id", "cart", Cart.class)).thenReturn(new Cart());

    String view = orderController.newOrderForm(session, model);

    assertThat(view).isEqualTo("redirect:/account");
  }

  /**
   * New order form with auth and cart returns new order view.
   */
  @Test
  void newOrderFormWithAuthAndCartReturnsNewOrderView() {
    // Use a mock HttpSession to supply a session ID; accountBean and cart are retrieved from Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();

    org.mybatis.jpetstore.domain.Account account = new org.mybatis.jpetstore.domain.Account();
    account.setUsername("j2ee");
    account.setFirstName("ABC");
    account.setLastName("Banner");
    account.setAddress1("901 San Antonio Road");
    account.setAddress2("MS UCUP02-207");
    account.setCity("Palo Alto");
    account.setState("CA");
    account.setZip("94303");
    account.setCountry("USA");
    AccountController.AccountSession accountSession = new AccountController.AccountSession(account, List.of(), true);
    when(redisSessionService.getAttribute("test-session-id", "accountBean",
        AccountController.AccountSession.class)).thenReturn(accountSession);

    Cart cart = new Cart();
    when(redisSessionService.getAttribute("test-session-id", "cart", Cart.class)).thenReturn(cart);

    String view = orderController.newOrderForm(session, model);

    assertThat(view).isEqualTo("order/NewOrderForm");
    assertThat(model.asMap().get("order")).isInstanceOf(org.mybatis.jpetstore.domain.Order.class);
    org.mybatis.jpetstore.domain.Order order = (org.mybatis.jpetstore.domain.Order) model.asMap().get("order");
    assertThat(order.getUsername()).isEqualTo("j2ee");
    assertThat(order.getShipAddress1()).isEqualTo("901 San Antonio Road");
    assertThat(model.asMap()).containsKey("creditCardTypes");
    // Verify the order was persisted to Redis (Memorystore) instead of the local HTTP session
    verify(redisSessionService, times(1)).setAttribute(
        org.mockito.ArgumentMatchers.eq("test-session-id"),
        org.mockito.ArgumentMatchers.eq("order"),
        org.mockito.ArgumentMatchers.any(org.mybatis.jpetstore.domain.Order.class));
  }

  /**
   * List orders with auth returns order list.
   */
  @Test
  void listOrdersWithAuthReturnsOrderList() {
    // Use a mock HttpSession to supply a session ID; accountBean is retrieved from Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();
    org.mybatis.jpetstore.domain.Account account = new org.mybatis.jpetstore.domain.Account();
    account.setUsername("j2ee");
    AccountController.AccountSession accountSession = new AccountController.AccountSession(account, List.of(), true);
    when(redisSessionService.getAttribute("test-session-id", "accountBean",
        AccountController.AccountSession.class)).thenReturn(accountSession);
    when(orderService.getOrdersByUsername("j2ee")).thenReturn(List.of());

    String view = orderController.listOrders(session, model);

    assertThat(view).isEqualTo("order/ListOrders");
    assertThat(model.asMap()).containsKey("orderList");
  }

  /**
   * Confirm order uses billing address from session.
   */
  @Test
  void confirmOrderUsesBillingAddressFromSession() {
    // When the ConfirmOrder form is submitted it only posts confirmed=true.
    // The @ModelAttribute Order is therefore all-null. The controller must use
    // the Redis order (which has the full billing address) and must NOT
    // overwrite it with the null-filled model attribute.
    // Use a mock HttpSession to supply a session ID; order state is stored in Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();

    org.mybatis.jpetstore.domain.Order sessionOrder = new org.mybatis.jpetstore.domain.Order();
    sessionOrder.setBillAddress1("901 San Antonio Road");
    sessionOrder.setBillCity("Palo Alto");
    sessionOrder.setBillState("CA");
    sessionOrder.setBillZip("94303");
    sessionOrder.setBillCountry("USA");
    sessionOrder.setBillToFirstName("ABC");
    sessionOrder.setBillToLastName("Banner");
    sessionOrder.setCreditCard("999 9999 9999 9999");
    sessionOrder.setExpiryDate("12/03");
    sessionOrder.setCardType("Visa");

    // Order is retrieved from Redis (Memorystore) instead of local HTTP session
    when(redisSessionService.getAttribute("test-session-id", "order",
        org.mybatis.jpetstore.domain.Order.class)).thenReturn(sessionOrder);

    // Simulate ConfirmOrder POST: empty @ModelAttribute (all fields null), confirmed=true
    org.mybatis.jpetstore.domain.Order emptyOrder = new org.mybatis.jpetstore.domain.Order();
    String view = orderController.newOrder(emptyOrder, false, true, session, model);

    assertThat(view).isEqualTo("order/ViewOrder");
    assertThat(model.asMap()).containsEntry("message", "Thank you, your order has been submitted.");

    // Verify insertOrder was called exactly once
    verify(orderService, times(1)).insertOrder(any(org.mybatis.jpetstore.domain.Order.class));

    // The Redis order must NOT have been overwritten with null
    assertThat(sessionOrder.getBillAddress1()).isEqualTo("901 San Antonio Road");
    assertThat(sessionOrder.getBillCity()).isEqualTo("Palo Alto");
  }

  /**
   * Shipping form submission does not overwrite billing fields.
   */
  @Test
  void shippingFormSubmissionDoesNotOverwriteBillingFields() {
    // ShippingForm only posts shipping fields (shipToFirstName etc.) + confirmed=false.
    // The billing/payment fields in @ModelAttribute Order are null (not in the form).
    // The controller must NOT overwrite the billing address stored in Redis
    // with these null values, or the subsequent insertOrder will fail with a
    // NOT NULL constraint violation on BILLADDR1.
    // Use a mock HttpSession to supply a session ID; order state is stored in Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();

    org.mybatis.jpetstore.domain.Order sessionOrder = new org.mybatis.jpetstore.domain.Order();
    sessionOrder.setBillAddress1("901 San Antonio Road");
    sessionOrder.setBillCity("Palo Alto");
    sessionOrder.setBillState("CA");
    sessionOrder.setBillZip("94303");
    sessionOrder.setBillCountry("USA");
    sessionOrder.setBillToFirstName("ABC");
    sessionOrder.setBillToLastName("Banner");
    sessionOrder.setCreditCard("999 9999 9999 9999");
    sessionOrder.setExpiryDate("12/03");
    sessionOrder.setCardType("Visa");

    // Order is retrieved from Redis (Memorystore) instead of local HTTP session
    when(redisSessionService.getAttribute("test-session-id", "order",
        org.mybatis.jpetstore.domain.Order.class)).thenReturn(sessionOrder);

    // Simulate ShippingForm POST: only shipping fields present, billing all null
    org.mybatis.jpetstore.domain.Order shippingFormOrder = new org.mybatis.jpetstore.domain.Order();
    shippingFormOrder.setShipToFirstName("ABC");
    shippingFormOrder.setShipToLastName("Banner");
    shippingFormOrder.setShipAddress1("901 San Antonio Road");
    shippingFormOrder.setShipAddress2("MS UCUP02-207");
    shippingFormOrder.setShipCity("Palo Alto");
    shippingFormOrder.setShipState("CA");
    shippingFormOrder.setShipZip("94303");
    shippingFormOrder.setShipCountry("USA");
    // billing/payment fields left null (not submitted by ShippingForm)

    String view = orderController.newOrder(shippingFormOrder, false, false, session, model);

    assertThat(view).isEqualTo("order/ConfirmOrder");

    // Billing fields in the Redis order must still have their original values
    assertThat(sessionOrder.getBillAddress1()).isEqualTo("901 San Antonio Road");
    assertThat(sessionOrder.getBillCity()).isEqualTo("Palo Alto");
    assertThat(sessionOrder.getCreditCard()).isEqualTo("999 9999 9999 9999");
    assertThat(sessionOrder.getCardType()).isEqualTo("Visa");

    // Shipping fields must have been updated from the form
    assertThat(sessionOrder.getShipAddress2()).isEqualTo("MS UCUP02-207");
  }

  /**
   * View order without auth redirects.
   */
  @Test
  void viewOrderWithoutAuthRedirects() {
    // Use a mock HttpSession to supply a session ID; accountBean is retrieved from Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();
    when(redisSessionService.getAttribute("test-session-id", "accountBean",
        AccountController.AccountSession.class)).thenReturn(null);

    String view = orderController.viewOrder(1, session, model);

    assertThat(view).isEqualTo("redirect:/account");
  }

  /**
   * View order with auth and matching username returns view order.
   */
  @Test
  void viewOrderWithAuthAndMatchingUsernameReturnsViewOrder() {
    // Use a mock HttpSession to supply a session ID; accountBean is retrieved from Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();
    org.mybatis.jpetstore.domain.Account account = new org.mybatis.jpetstore.domain.Account();
    account.setUsername("j2ee");
    AccountController.AccountSession accountSession = new AccountController.AccountSession(account, List.of(), true);
    when(redisSessionService.getAttribute("test-session-id", "accountBean",
        AccountController.AccountSession.class)).thenReturn(accountSession);

    org.mybatis.jpetstore.domain.Order order = new org.mybatis.jpetstore.domain.Order();
    order.setUsername("j2ee");
    when(orderService.getOrder(1)).thenReturn(order);

    String view = orderController.viewOrder(1, session, model);

    assertThat(view).isEqualTo("order/ViewOrder");
    assertThat(model.asMap()).containsEntry("order", order);
  }

  /**
   * View order with auth but different username returns error.
   */
  @Test
  void viewOrderWithAuthButDifferentUsernameReturnsError() {
    // Use a mock HttpSession to supply a session ID; accountBean is retrieved from Redis
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn("test-session-id");
    Model model = new ExtendedModelMap();
    org.mybatis.jpetstore.domain.Account account = new org.mybatis.jpetstore.domain.Account();
    account.setUsername("j2ee");
    AccountController.AccountSession accountSession = new AccountController.AccountSession(account, List.of(), true);
    when(redisSessionService.getAttribute("test-session-id", "accountBean",
        AccountController.AccountSession.class)).thenReturn(accountSession);

    org.mybatis.jpetstore.domain.Order order = new org.mybatis.jpetstore.domain.Order();
    order.setUsername("someoneelse");
    when(orderService.getOrder(1)).thenReturn(order);

    String view = orderController.viewOrder(1, session, model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsEntry("message", "You may only view your own orders.");
  }
}
