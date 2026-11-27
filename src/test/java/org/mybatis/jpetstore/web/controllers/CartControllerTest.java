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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.service.CatalogService;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

/**
 * The Class CartControllerTest.
 *
 * <p>Unit tests for {@link CartController}. The {@link HttpSession} is mocked in these
 * tests to simulate the Redis-backed session store (GCP Memorystore for Redis via Spring Session).
 * In production, Spring Session transparently replaces the standard {@link HttpSession}
 * implementation with a Redis-backed one, so controller code using {@code session.getAttribute()}
 * and {@code session.setAttribute()} works identically whether backed by in-memory or Redis storage.
 */
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

  /** The catalog service. */
  @Mock
  private CatalogService catalogService;

  /** The cart controller. */
  @InjectMocks
  private CartController cartController;

  /**
   * View cart returns cart view.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). The {@code cart} attribute
   * is retrieved from GCP Memorystore for Redis in production.
   */
  @Test
  void viewCartReturnsCartView() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Cart cart = new Cart();
    when(session.getAttribute("cart")).thenReturn(cart);
    Model model = new ExtendedModelMap();

    String view = cartController.viewCart(session, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(model.asMap()).containsKey("cart");
  }

  /**
   * Add item with null id returns error.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session).
   */
  @Test
  void addItemWithNullIdReturnsError() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Model model = new ExtendedModelMap();

    String view = cartController.addItemToCart(null, session, model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsKey("message");
  }

  /**
   * Add item with new id fetches item from catalog and adds to cart.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). In production, the
   * {@code cart} attribute is stored in GCP Memorystore for Redis.
   */
  @Test
  void addItemWithNewIdFetchesItemFromCatalogAndAddsToCart() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    when(session.getAttribute("cart")).thenReturn(cart);

    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    when(catalogService.isItemInStock("EST-1")).thenReturn(true);
    when(catalogService.getItem("EST-1")).thenReturn(item);

    String view = cartController.addItemToCart("EST-1", session, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.containsItemId("EST-1")).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(1);
    assertThat(model.asMap().get("cart")).isSameAs(cart);
  }

  /**
   * Add item with existing id increments quantity without calling catalog.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). In production, the
   * updated {@code cart} attribute is stored in GCP Memorystore for Redis.
   */
  @Test
  void addItemWithExistingIdIncrementsQuantityWithoutCallingCatalog() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(session.getAttribute("cart")).thenReturn(cart);

    String view = cartController.addItemToCart("EST-1", session, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(2);
    org.mockito.Mockito.verifyNoInteractions(catalogService);
  }

  /**
   * Remove item with null id returns error.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session).
   */
  @Test
  void removeItemWithNullIdReturnsError() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Model model = new ExtendedModelMap();

    String view = cartController.removeItemFromCart(null, session, model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsKey("message");
  }

  /**
   * Remove item with existing id removes item from cart.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). In production, the
   * updated {@code cart} attribute is stored in GCP Memorystore for Redis.
   */
  @Test
  void removeItemWithExistingIdRemovesItemFromCart() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(session.getAttribute("cart")).thenReturn(cart);

    String view = cartController.removeItemFromCart("EST-1", session, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.containsItemId("EST-1")).isFalse();
    assertThat(model.asMap().get("cart")).isSameAs(cart);
  }

  /**
   * Remove item with non existent id returns error.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). A null return from
   * {@code session.getAttribute("cart")} indicates no active cart in Redis.
   */
  @Test
  void removeItemWithNonExistentIdReturnsError() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    when(session.getAttribute("cart")).thenReturn(cart);

    String view = cartController.removeItemFromCart("NOT-THERE", session, model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsKey("message");
  }

  /**
   * Update cart quantities updates quantity.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). In production, the
   * updated {@code cart} attribute is stored in GCP Memorystore for Redis.
   */
  @Test
  void updateCartQuantitiesUpdatesQuantity() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(session.getAttribute("cart")).thenReturn(cart);
    when(request.getParameter("EST-1")).thenReturn("3");

    String view = cartController.updateCartQuantities(request, session, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.containsItemId("EST-1")).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(3);
  }

  /**
   * Update cart quantities removes item when quantity less than one.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). In production, the
   * updated {@code cart} attribute is stored in GCP Memorystore for Redis.
   */
  @Test
  void updateCartQuantitiesRemovesItemWhenQuantityLessThanOne() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(session.getAttribute("cart")).thenReturn(cart);
    when(request.getParameter("EST-1")).thenReturn("0");

    String view = cartController.updateCartQuantities(request, session, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.getCartItemList()).isEmpty();
  }

  /**
   * Update cart quantities ignores invalid numeric input.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). In production, the
   * {@code cart} attribute is stored in GCP Memorystore for Redis.
   */
  @Test
  void updateCartQuantitiesIgnoresInvalidNumericInput() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(session.getAttribute("cart")).thenReturn(cart);
    when(request.getParameter("EST-1")).thenReturn("not-a-number");

    String view = cartController.updateCartQuantities(request, session, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(1);
  }

  /**
   * Check out returns checkout view with cart from session.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). The {@code cart} attribute
   * is retrieved from GCP Memorystore for Redis in production.
   */
  @Test
  void checkOutReturnsCheckoutViewWithCartFromSession() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    Cart cart = new Cart();
    when(session.getAttribute("cart")).thenReturn(cart);
    Model model = new ExtendedModelMap();

    String view = cartController.checkOut(session, model);

    assertThat(view).isEqualTo("cart/Checkout");
    assertThat(model.asMap().get("cart")).isSameAs(cart);
  }

  /**
   * Check out with no existing cart creates new cart in session.
   *
   * <p>The mocked {@link HttpSession} simulates the Redis-backed session
   * (GCP Memorystore for Redis via Spring Session). Calling
   * {@code session.setAttribute("cart", ...)} stores the new cart in
   * GCP Memorystore for Redis in production.
   */
  @Test
  void checkOutWithNoExistingCartCreatesNewCartInSession() {
    // Mock simulates the Redis-backed HttpSession (GCP Memorystore for Redis via Spring Session)
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("cart")).thenReturn(null);
    Model model = new ExtendedModelMap();

    String view = cartController.checkOut(session, model);

    assertThat(view).isEqualTo("cart/Checkout");
    assertThat(model.asMap().get("cart")).isInstanceOf(Cart.class);
    org.mockito.Mockito.verify(session).setAttribute(org.mockito.ArgumentMatchers.eq("cart"),
        org.mockito.ArgumentMatchers.any(Cart.class));
  }
}
