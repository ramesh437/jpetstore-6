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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mybatis.jpetstore.domain.Cart;
import org.mybatis.jpetstore.domain.Item;
import org.mybatis.jpetstore.service.CatalogService;
import org.mybatis.jpetstore.web.session.SessionStore;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

/**
 * The Class CartControllerTest.
 */
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

  /** The external session store. */
  @Mock
  private SessionStore sessionStore;

  /** The catalog service. */
  @Mock
  private CatalogService catalogService;

  /** The cart controller. */
  @InjectMocks
  private CartController cartController;

  /**
   * View cart returns cart view.
   */
  @Test
  void viewCartReturnsCartView() {
    Cart cart = new Cart();
    when(sessionStore.get("cart")).thenReturn(cart);
    Model model = new ExtendedModelMap();

    String view = cartController.viewCart(model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(model.asMap()).containsKey("cart");
  }

  /**
   * Add item with null id returns error.
   */
  @Test
  void addItemWithNullIdReturnsError() {
    Model model = new ExtendedModelMap();

    String view = cartController.addItemToCart(null, model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsKey("message");
  }

  /**
   * Add item with new id fetches item from catalog and adds to cart.
   */
  @Test
  void addItemWithNewIdFetchesItemFromCatalogAndAddsToCart() {
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    when(sessionStore.get("cart")).thenReturn(cart);

    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    when(catalogService.isItemInStock("EST-1")).thenReturn(true);
    when(catalogService.getItem("EST-1")).thenReturn(item);

    String view = cartController.addItemToCart("EST-1", model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.containsItemId("EST-1")).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(1);
    assertThat(model.asMap().get("cart")).isSameAs(cart);
  }

  /**
   * Add item with existing id increments quantity without calling catalog.
   */
  @Test
  void addItemWithExistingIdIncrementsQuantityWithoutCallingCatalog() {
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(sessionStore.get("cart")).thenReturn(cart);

    String view = cartController.addItemToCart("EST-1", model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(2);
    org.mockito.Mockito.verifyNoInteractions(catalogService);
  }

  /**
   * Remove item with null id returns error.
   */
  @Test
  void removeItemWithNullIdReturnsError() {
    Model model = new ExtendedModelMap();

    String view = cartController.removeItemFromCart(null, model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsKey("message");
  }

  /**
   * Remove item with existing id removes item from cart.
   */
  @Test
  void removeItemWithExistingIdRemovesItemFromCart() {
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(sessionStore.get("cart")).thenReturn(cart);

    String view = cartController.removeItemFromCart("EST-1", model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.containsItemId("EST-1")).isFalse();
    assertThat(model.asMap().get("cart")).isSameAs(cart);
  }

  /**
   * Remove item with non existent id returns error.
   */
  @Test
  void removeItemWithNonExistentIdReturnsError() {
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    when(sessionStore.get("cart")).thenReturn(cart);

    String view = cartController.removeItemFromCart("NOT-THERE", model);

    assertThat(view).isEqualTo("common/Error");
    assertThat(model.asMap()).containsKey("message");
  }

  /**
   * Update cart quantities updates quantity.
   */
  @Test
  void updateCartQuantitiesUpdatesQuantity() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(sessionStore.get("cart")).thenReturn(cart);
    when(request.getParameter("EST-1")).thenReturn("3");

    String view = cartController.updateCartQuantities(request, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.containsItemId("EST-1")).isTrue();
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(3);
  }

  /**
   * Update cart quantities removes item when quantity less than one.
   */
  @Test
  void updateCartQuantitiesRemovesItemWhenQuantityLessThanOne() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(sessionStore.get("cart")).thenReturn(cart);
    when(request.getParameter("EST-1")).thenReturn("0");

    String view = cartController.updateCartQuantities(request, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.getCartItemList()).isEmpty();
  }

  /**
   * Update cart quantities ignores invalid numeric input.
   */
  @Test
  void updateCartQuantitiesIgnoresInvalidNumericInput() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    Model model = new ExtendedModelMap();

    Cart cart = new Cart();
    Item item = new Item();
    item.setItemId("EST-1");
    item.setListPrice(new java.math.BigDecimal("10.00"));
    cart.addItem(item, true);
    when(sessionStore.get("cart")).thenReturn(cart);
    when(request.getParameter("EST-1")).thenReturn("not-a-number");

    String view = cartController.updateCartQuantities(request, model);

    assertThat(view).isEqualTo("cart/Cart");
    assertThat(cart.getCartItemList().get(0).getQuantity()).isEqualTo(1);
  }

  /**
   * Check out returns checkout view with cart from session.
   */
  @Test
  void checkOutReturnsCheckoutViewWithCartFromSession() {
    Cart cart = new Cart();
    when(sessionStore.get("cart")).thenReturn(cart);
    Model model = new ExtendedModelMap();

    String view = cartController.checkOut(model);

    assertThat(view).isEqualTo("cart/Checkout");
    assertThat(model.asMap().get("cart")).isSameAs(cart);
  }

  /**
   * Check out with no existing cart creates new cart in session.
   */
  @Test
  void checkOutWithNoExistingCartCreatesNewCartInSession() {
    when(sessionStore.get("cart")).thenReturn(null);
    Model model = new ExtendedModelMap();

    String view = cartController.checkOut(model);

    assertThat(view).isEqualTo("cart/Checkout");
    assertThat(model.asMap().get("cart")).isInstanceOf(Cart.class);
    org.mockito.Mockito.verify(sessionStore).set(org.mockito.ArgumentMatchers.eq("cart"),
        org.mockito.ArgumentMatchers.any(Cart.class));
  }
}
