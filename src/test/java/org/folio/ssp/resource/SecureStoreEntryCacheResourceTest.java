package org.folio.ssp.resource;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;
import static org.folio.ssp.support.AssertionUtils.assertCached;
import static org.folio.ssp.support.AssertionUtils.assertNotCached;
import static org.folio.ssp.support.TestConstants.KEY1;
import static org.folio.ssp.support.TestConstants.KEY2;
import static org.folio.ssp.support.TestConstants.VALUE1;
import static org.folio.ssp.support.TestConstants.VALUE2;
import static org.folio.ssp.support.TestUtils.await;
import static org.folio.ssp.support.TestUtils.putInCache;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@UnitTest
@QuarkusTest
@TestHTTPEndpoint(SecureStoreEntryCacheResource.class)
class SecureStoreEntryCacheResourceTest {

  @Inject @CacheName(ENTRY_CACHE) Cache entryCache;

  @AfterEach
  void tearDown() {
    await(entryCache.invalidateAll());
  }

  @Test
  void getAllEntryKeys_positive() {
    putInCache(entryCache, KEY1, VALUE1);
    putInCache(entryCache, KEY2, VALUE2);

    given()
      .when().get()
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "", hasSize(2),
        "[0]", is(KEY1),
        "[1]", is(KEY2)
      );
  }

  @Test
  void getAllEntryKeys_positive_empty() {
    given()
      .when().get()
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(containsString(APPLICATION_JSON))
      .body("", hasSize(0));
  }

  @Test
  void invalidate_positive() throws Exception {
    putInCache(entryCache, KEY1, VALUE1);
    putInCache(entryCache, KEY2, VALUE2);

    given()
      .when().delete("/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertNotCached(entryCache, KEY1);
    assertCached(entryCache, KEY2, VALUE2);
  }

  @Test
  void invalidate_positive_notCachedKey() throws Exception {
    putInCache(entryCache, KEY1, VALUE1);

    given()
      .when().delete("/{key}", "notCachedKey")
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertCached(entryCache, KEY1, VALUE1);
  }

  @Test
  void invalidate_negative_blankKey() {
    given()
      .when().delete("/{key}", StringUtils.SPACE)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_BAD_REQUEST))
      .contentType(is(APPLICATION_JSON))
      .body(
        "title", is("Constraint Violation"),
        "status", is(SC_BAD_REQUEST),
        "violations[0].field", is("invalidateEntry.key"),
        "violations[0].message", is("must not be blank")
      );
  }

  @Test
  void invalidateAll_positive() throws Exception {
    putInCache(entryCache, KEY1, VALUE1);
    putInCache(entryCache, KEY2, VALUE2);

    given()
      .when().delete()
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertNotCached(entryCache, KEY1);
    assertNotCached(entryCache, KEY2);
  }

  @Test
  void invalidateAll_positive_withEmptyCache() {
    given()
      .when().delete()
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(entryCache.as(CaffeineCache.class).keySet()).isEmpty();
  }
}
