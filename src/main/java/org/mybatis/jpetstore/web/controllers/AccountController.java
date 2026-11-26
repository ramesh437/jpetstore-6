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

import java.util.List;

import javax.servlet.http.HttpSession;

import org.mybatis.jpetstore.domain.Account;
import org.mybatis.jpetstore.domain.Product;
import org.mybatis.jpetstore.service.AccountService;
import org.mybatis.jpetstore.service.CatalogService;
import org.mybatis.jpetstore.session.RedisSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.ImmutableList;

/**
 * The Class AccountController.
 *
 * <p>Session state (accountBean) is stored in Google Cloud Memorystore for Redis
 * via {@link RedisSessionService}, enabling stateless, horizontally-scalable
 * application instances without server affinity.
 */
@Controller
@RequestMapping("/account")
public class AccountController {

  /** The signon view. */
  private static final String SIGNON_VIEW = "account/SignonForm";
  /** The new account view. */
  private static final String NEW_ACCOUNT_VIEW = "account/NewAccountForm";
  /** The edit account view. */
  private static final String EDIT_ACCOUNT_VIEW = "account/EditAccountForm";

  /** Redis key name for the account session attribute. */
  private static final String ACCOUNT_BEAN_KEY = "accountBean";

  /**
   * The language list. Bounded, immutable collection using Guava ImmutableList to prevent
   * unbounded memory growth in GKE containers with strict memory limits.
   */
  private static final ImmutableList<String> LANGUAGE_LIST = ImmutableList.of("english", "japanese");
  /**
   * The category list. Bounded, immutable collection using Guava ImmutableList to prevent
   * unbounded memory growth in GKE containers with strict memory limits.
   */
  private static final ImmutableList<String> CATEGORY_LIST = ImmutableList.of("FISH", "DOGS", "REPTILES", "CATS", "BIRDS");

  /** The account service. */
  @Autowired
  private AccountService accountService;
  /** The catalog service. */
  @Autowired
  private CatalogService catalogService;
  /**
   * Redis-backed session service — replaces direct HttpSession attribute storage
   * with Google Cloud Memorystore for Redis to support stateless horizontal scaling.
   */
  @Autowired
  private RedisSessionService redisSessionService;

  /**
   * Signon form.
   *
   * @return the string
   */
  @GetMapping({ "", "/" })
  public String signonForm() {
    return SIGNON_VIEW;
  }

  /**
   * Signon.
   *
   * <p>On successful authentication the {@code accountBean} is persisted to
   * Google Cloud Memorystore for Redis (via {@link RedisSessionService}) rather
   * than the local HTTP session, so any application instance can serve
   * subsequent requests for this user.
   *
   * @param username the username
   * @param password the password
   * @param session  the HTTP session (used only to obtain the session ID)
   * @param model    the model
   * @return the string
   */
  @PostMapping("/signon")
  public String signon(@RequestParam("username") String username, @RequestParam("password") String password,
      HttpSession session, Model model) {
    Account account = accountService.getAccount(username, password);
    if (account == null) {
      model.addAttribute("message", "Invalid username or password.  Signon failed.");
      return SIGNON_VIEW;
    }
    account.setPassword(null);
    List<Product> myList = catalogService.getProductListByCategory(account.getFavouriteCategoryId());
    // Store accountBean in Redis (Memorystore) instead of local HTTP session
    redisSessionService.setAttribute(session.getId(), ACCOUNT_BEAN_KEY, new AccountSession(account, myList, true));
    return "redirect:/catalog";
  }

  /**
   * Signoff.
   *
   * <p>Invalidates the Redis session data for this user and then invalidates
   * the local HTTP session so that all state is cleanly removed.
   *
   * @param session the HTTP session
   * @return the string
   */
  @GetMapping("/signoff")
  public String signoff(HttpSession session) {
    // Remove all Redis-backed session data for this session ID
    redisSessionService.invalidateSession(session.getId());
    session.invalidate();
    return "redirect:/catalog";
  }

  /**
   * New account form.
   *
   * @param model the model
   * @return the string
   */
  @GetMapping("/new")
  public String newAccountForm(Model model) {
    model.addAttribute("languages", LANGUAGE_LIST);
    model.addAttribute("categories", CATEGORY_LIST);
    model.addAttribute("account", new Account());
    return NEW_ACCOUNT_VIEW;
  }

  /**
   * New account.
   *
   * @param account the account
   * @return the string
   */
  @PostMapping("/new")
  public String newAccount(@ModelAttribute Account account) {
    accountService.insertAccount(account);
    return "redirect:/catalog";
  }

  /**
   * Edit account form.
   *
   * <p>Reads the {@code accountBean} from Google Cloud Memorystore for Redis
   * rather than the local HTTP session.
   *
   * @param session the HTTP session (used only to obtain the session ID)
   * @param model   the model
   * @return the string
   */
  @GetMapping("/edit")
  public String editAccountForm(HttpSession session, Model model) {
    // Retrieve accountBean from Redis (Memorystore) instead of local HTTP session
    AccountSession accountSession = redisSessionService.getAttribute(
        session.getId(), ACCOUNT_BEAN_KEY, AccountSession.class);
    if (accountSession != null) {
      model.addAttribute("account", accountSession.getAccount());
    }
    model.addAttribute("languages", LANGUAGE_LIST);
    model.addAttribute("categories", CATEGORY_LIST);
    return EDIT_ACCOUNT_VIEW;
  }

  /**
   * Edit account.
   *
   * <p>Persists the updated {@code accountBean} to Google Cloud Memorystore for
   * Redis rather than the local HTTP session.
   *
   * @param account the account
   * @param session the HTTP session (used only to obtain the session ID)
   * @return the string
   */
  @PostMapping("/edit")
  public String editAccount(@ModelAttribute Account account, HttpSession session) {
    accountService.updateAccount(account);
    Account updatedAccount = accountService.getAccount(account.getUsername());
    List<Product> myList = catalogService.getProductListByCategory(updatedAccount.getFavouriteCategoryId());
    // Persist updated accountBean to Redis (Memorystore) instead of local HTTP session
    redisSessionService.setAttribute(session.getId(), ACCOUNT_BEAN_KEY,
        new AccountSession(updatedAccount, myList, true));
    return "redirect:/account/edit";
  }

  /**
   * Inner class to hold account session data, compatible with JSP ${sessionScope.accountBean.*} references.
   */
  public static class AccountSession implements java.io.Serializable {
    /** The serial version uid. */
    private static final long serialVersionUID = 1L;
    /** The account. */
    private final Account account;
    /** The my list. */
    private final List<Product> myList;
    /** The authenticated. */
    private final boolean authenticated;

    /**
     * Instantiates a new account session.
     *
     * @param account       the account
     * @param myList        the my list
     * @param authenticated the authenticated
     */
    public AccountSession(Account account, List<Product> myList, boolean authenticated) {
      this.account = account;
      this.myList = myList;
      this.authenticated = authenticated;
    }

    /**
     * Gets the account.
     *
     * @return the account
     */
    public Account getAccount() {
      return account;
    }

    /**
     * Gets the my list.
     *
     * @return the my list
     */
    public List<Product> getMyList() {
      return myList;
    }

    /**
     * Checks if is authenticated.
     *
     * @return true, if successful
     */
    public boolean isAuthenticated() {
      return authenticated && account != null && account.getUsername() != null;
    }
  }
}
