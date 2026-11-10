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
package org.mybatis.jpetstore.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.jpetstore.domain.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class AccountMapperTest.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MapperTestContext.class)
@Transactional
class AccountMapperTest {

  /** The mapper. */
  @Autowired
  private AccountMapper mapper;

  /** The jdbc template. */
  @Autowired
  private JdbcTemplate jdbcTemplate;

  /**
   * Gets the account by username.
   */
  @Test
  void getAccountByUsername() {
    // given
    String username = "j2ee";

    // when
    Account account = mapper.getAccountByUsername(username);

    // then
    assertThat(account.getUsername()).isEqualTo("j2ee");
    assertThat(account.getEmail()).isEqualTo("yourname@yourdomain.com");
    assertThat(account.getFirstName()).isEqualTo("ABC");
    assertThat(account.getLastName()).isEqualTo("XYX");
    assertThat(account.getStatus()).isEqualTo("OK");
    assertThat(account.getAddress1()).isEqualTo("901 San Antonio Road");
    assertThat(account.getAddress2()).isEqualTo("MS UCUP02-206");
    assertThat(account.getCity()).isEqualTo("Palo Alto");
    assertThat(account.getState()).isEqualTo("CA");
    assertThat(account.getZip()).isEqualTo("94303");
    assertThat(account.getCountry()).isEqualTo("USA");
    assertThat(account.getPhone()).isEqualTo("555-555-5555");
    assertThat(account.getLanguagePreference()).isEqualTo("english");
    assertThat(account.getFavouriteCategoryId()).isEqualTo("DOGS");
    assertThat(account.isListOption()).isTrue();
    assertThat(account.isBannerOption()).isTrue();
    assertThat(account.getBannerName()).isEqualTo("<image src=\"/jpetstore/images/banner_dogs.gif\">");

  }

  /**
   * Gets the account by username and password.
   */
  @Test
  void getAccountByUsernameAndPassword() {
    // given
    String username = "ACID";
    String password = resolveSecret("JPETSTORE_TEST_ACID_PASSWORD", "jpetstore.test.acid-password", username);

    // when
    Account account = mapper.getAccountByUsernameAndPassword(username, password);

    // then
    assertThat(account.getUsername()).isEqualTo("ACID");
    assertThat(account.getEmail()).isEqualTo("acid@yourdomain.com");
    assertThat(account.getFirstName()).isEqualTo("ABC");
    assertThat(account.getLastName()).isEqualTo("XYX");
    assertThat(account.getStatus()).isEqualTo("OK");
    assertThat(account.getAddress1()).isEqualTo("901 San Antonio Road");
    assertThat(account.getAddress2()).isEqualTo("MS UCUP02-206");
    assertThat(account.getCity()).isEqualTo("Palo Alto");
    assertThat(account.getState()).isEqualTo("CA");
    assertThat(account.getZip()).isEqualTo("94303");
    assertThat(account.getCountry()).isEqualTo("USA");
    assertThat(account.getPhone()).isEqualTo("555-555-5555");
    assertThat(account.getLanguagePreference()).isEqualTo("english");
    assertThat(account.getFavouriteCategoryId()).isEqualTo("CATS");
    assertThat(account.isListOption()).isTrue();
    assertThat(account.isBannerOption()).isTrue();
    assertThat(account.getBannerName()).isEqualTo("<image src=\"/jpetstore/images/banner_cats.gif\">");

  }

  /**
   * Resolves a credential from Google Secret Manager instead of hard-coding it in source. Resolution order: (1) the
   * secret identified by the <code>JPETSTORE_TEST_ACID_PASSWORD_SECRET</code> environment variable (format
   * <code>projects/{project}/secrets/{name}/versions/{version}</code>) is read from Google Secret Manager; (2) a value
   * injected as an environment variable or system property (e.g. Secret Manager mounted/injected by the platform);
   * (3) when no secret is supplied (e.g. local runs against the embedded HSQLDB fixture), the fixture's convention
   * where the seeded password equals the user name.
   *
   * @param envName
   *          the environment variable name
   * @param propertyName
   *          the system property name
   * @param fixtureDefault
   *          the value used when no secret is configured
   * @return the resolved secret value
   */
  private static String resolveSecret(String envName, String propertyName, String fixtureDefault) {
    String secretVersion = System.getenv(envName + "_SECRET");
    if (secretVersion != null && !secretVersion.isEmpty()) {
      try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
        return client.accessSecretVersion(secretVersion).getPayload().getData().toStringUtf8();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read secret from Google Secret Manager: " + secretVersion, e);
      }
    }
    String value = System.getenv(envName);
    return value != null && !value.isEmpty() ? value : System.getProperty(propertyName, fixtureDefault);
  }

  /**
   * Insert account.
   */
  @Test
  void insertAccount() {

    // given
    Account account = new Account();
    account.setUsername("mybatis");
    account.setEmail("mybatis@example.com");
    account.setFirstName("My");
    account.setLastName("Batis");
    account.setStatus("NG");
    account.setAddress1("Address 1");
    account.setAddress2("Address 2");
    account.setCity("City");
    account.setState("ST");
    account.setZip("99001");
    account.setCountry("JPN");
    account.setPhone("09012345678");

    // when
    mapper.insertAccount(account);

    // then
    Map<String, Object> recordMap = jdbcTemplate.queryForMap("SELECT * FROM account WHERE userid = ?", "mybatis");
    assertThat(recordMap).hasSize(12).containsEntry("USERID", account.getUsername())
        .containsEntry("EMAIL", account.getEmail()).containsEntry("FIRSTNAME", account.getFirstName())
        .containsEntry("LASTNAME", account.getLastName()).containsEntry("STATUS", account.getStatus())
        .containsEntry("ADDR1", account.getAddress1()).containsEntry("ADDR2", account.getAddress2())
        .containsEntry("CITY", account.getCity()).containsEntry("STATE", account.getState())
        .containsEntry("ZIP", account.getZip()).containsEntry("COUNTRY", account.getCountry())
        .containsEntry("PHONE", account.getPhone());
  }

  /**
   * Insert profile.
   */
  @Test
  void insertProfile() {

    // given
    Account account = new Account();
    account.setUsername("mybatis");
    account.setLanguagePreference("japanese");
    account.setFavouriteCategoryId("C01");
    account.setListOption(true);
    account.setBannerOption(false);

    // when
    mapper.insertProfile(account);

    // then
    Map<String, Object> recordMap = jdbcTemplate.queryForMap("SELECT * FROM profile WHERE userid = ?", "mybatis");

    assertThat(recordMap).hasSize(5).containsEntry("USERID", account.getUsername())
        .containsEntry("LANGPREF", account.getLanguagePreference())
        .containsEntry("FAVCATEGORY", account.getFavouriteCategoryId()).containsEntry("MYLISTOPT", 1)
        .containsEntry("BANNEROPT", 0);
  }

  /**
   * Insert signon.
   */
  @Test
  void insertSignon() {

    // given
    Account account = new Account();
    account.setUsername("mybatis");
    account.setPassword("password");

    // when
    mapper.insertSignon(account);

    // then
    Map<String, Object> recordMap = jdbcTemplate.queryForMap("SELECT * FROM signon WHERE username = ?", "mybatis");

    assertThat(recordMap).hasSize(2).containsEntry("USERNAME", account.getUsername()).containsEntry("PASSWORD",
        account.getPassword());
  }

  /**
   * Update account.
   */
  @Test
  void updateAccount() {

    // given
    Account account = new Account();
    account.setUsername("j2ee");
    account.setEmail("mybatis@example.com");
    account.setFirstName("My");
    account.setLastName("Batis");
    account.setStatus("NG");
    account.setAddress1("Address 1");
    account.setAddress2("Address 2");
    account.setCity("City");
    account.setState("ST");
    account.setZip("99001");
    account.setCountry("JPN");
    account.setPhone("09012345678");

    // when
    mapper.updateAccount(account);

    // then
    Map<String, Object> recordMap = jdbcTemplate.queryForMap("SELECT * FROM account WHERE userid = ?", "j2ee");

    assertThat(recordMap).hasSize(12).containsEntry("USERID", account.getUsername())
        .containsEntry("EMAIL", account.getEmail()).containsEntry("FIRSTNAME", account.getFirstName())
        .containsEntry("LASTNAME", account.getLastName()).containsEntry("STATUS", account.getStatus())
        .containsEntry("ADDR1", account.getAddress1()).containsEntry("ADDR2", account.getAddress2())
        .containsEntry("CITY", account.getCity()).containsEntry("STATE", account.getState())
        .containsEntry("ZIP", account.getZip()).containsEntry("COUNTRY", account.getCountry())
        .containsEntry("PHONE", account.getPhone());
  }

  /**
   * Update profile.
   */
  @Test
  void updateProfile() {

    // given
    Account account = new Account();
    account.setUsername("j2ee");
    account.setLanguagePreference("japanese");
    account.setFavouriteCategoryId("C01");
    account.setListOption(false);
    account.setBannerOption(false);

    // when
    mapper.updateProfile(account);

    // then
    Map<String, Object> recordMap = jdbcTemplate.queryForMap("SELECT * FROM profile WHERE userid = ?", "j2ee");

    assertThat(recordMap).hasSize(5).containsEntry("USERID", account.getUsername())
        .containsEntry("LANGPREF", account.getLanguagePreference())
        .containsEntry("FAVCATEGORY", account.getFavouriteCategoryId()).containsEntry("MYLISTOPT", 0)
        .containsEntry("BANNEROPT", 0);
  }

  /**
   * Update signon.
   */
  @Test
  void updateSignon() {

    // given
    Account account = new Account();
    account.setUsername("j2ee");
    account.setPassword("password");

    // when
    mapper.updateSignon(account);

    // then
    Map<String, Object> recordMap = jdbcTemplate.queryForMap("SELECT * FROM signon WHERE username = ?", "j2ee");

    assertThat(recordMap).hasSize(2).containsEntry("USERNAME", account.getUsername()).containsEntry("PASSWORD",
        account.getPassword());
  }

}
