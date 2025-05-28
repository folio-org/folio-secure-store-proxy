package org.folio.ssp.resource;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;
import static org.folio.ssp.model.error.ErrorCode.VALIDATION_ERROR;
import static org.folio.ssp.support.AssertionUtils.assertCached;
import static org.folio.ssp.support.AssertionUtils.assertNotCached;
import static org.folio.ssp.support.TestConstants.KEY1;
import static org.folio.ssp.support.TestConstants.VALUE1;
import static org.folio.ssp.support.TestConstants.VALUE2;
import static org.folio.ssp.support.TestUtils.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.folio.ssp.model.SecureStoreEntry;
import org.folio.ssp.model.error.ErrorCode;
import org.folio.ssp.support.profile.InMemorySecureStoreTestProfile;
import org.folio.support.types.UnitTest;
import org.folio.tools.store.impl.InMemorySecureStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@UnitTest
@QuarkusTest
@TestProfile(InMemorySecureStoreTestProfile.class)
@TestHTTPEndpoint(SecureStoreEntryResource.class)
class SecureStoreEntryResourceTest {

  @Inject InMemorySecureStore secureStore;
  @Inject @CacheName(ENTRY_CACHE) Cache entryCache;

  @AfterEach
  void tearDown() {
    secureStore.getData().clear();
    await(entryCache.invalidateAll());
  }

  @Test
  void getEntry_positive() throws Exception {
    secureStore.set(KEY1, VALUE1);

    given()
      .when().get("{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "key", is(KEY1),
        "value", is(VALUE1)
      );

    assertCached(entryCache, KEY1, VALUE1);
  }

  @Test
  void getEntry_negative_notFound() {
    given()
      .when().get("{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NOT_FOUND))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "errors[0].type", is("NotFoundException"),
        "errors[0].code", is(ErrorCode.NOT_FOUND_ERROR.getValue()),
        "errors[0].message", is("Entry not found: key = " + KEY1),
        "total_records", is(1)
      );
  }

  @Test
  void getEntry_negative_blankKey() {
    given()
      .when().get("/{key}", SPACE)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_BAD_REQUEST))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "errors[0].type", is("ConstraintViolationException"),
        "errors[0].code", is(VALIDATION_ERROR.getValue()),
        "errors[0].message", is("Validation failed"),
        "errors[0].parameters[0].key", is("getEntry.key"),
        "errors[0].parameters[0].value", is("Key must not be blank"),
        "total_records", is(1)
      );
  }

  @Test
  void setEntry_positive_entryCreated() throws Exception {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE1);

    given()
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put("{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.get(KEY1)).isEqualTo(VALUE1);
    assertCached(entryCache, KEY1, VALUE1);
  }

  @Test
  void setEntry_positive_entryUpdated() throws Exception {
    secureStore.set(KEY1, VALUE1);

    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE2);

    given()
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put("{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.get(KEY1)).isEqualTo(VALUE2);
    assertCached(entryCache, KEY1, VALUE2);
  }

  @Test
  void setEntry_negative_blankKey() {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE1);

    given()
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put("{key}", SPACE)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_BAD_REQUEST))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "errors[0].type", is("ConstraintViolationException"),
        "errors[0].code", is(VALIDATION_ERROR.getValue()),
        "errors[0].message", is("Validation failed"),
        "errors[0].parameters[0].key", is("setEntry.key"),
        "errors[0].parameters[0].value", is("Key must not be blank"),
        "total_records", is(1)
      );
  }

  @Test
  void setEntry_negative_blankValue() {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, null);

    given()
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put("{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_BAD_REQUEST))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "errors[0].type", is("ConstraintViolationException"),
        "errors[0].code", is(VALIDATION_ERROR.getValue()),
        "errors[0].message", is("Validation failed"),
        "errors[0].parameters[0].key", is("setEntry.entry.value"),
        "errors[0].parameters[0].value", is("Value must not be blank"),
        "total_records", is(1)
      );
  }

  @Test
  void deleteEntry_positive() throws Exception {
    secureStore.set(KEY1, VALUE1);

    given()
      .when().delete("{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.getData().get(KEY1)).isNull();
    assertNotCached(entryCache, KEY1);
  }

  @Test
  void deleteEntry_positive_notStoredEntry() throws Exception {
    given()
      .when().delete("{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.getData().get(KEY1)).isNull();
    assertNotCached(entryCache, KEY1);
  }

  @Test
  void deleteEntry_negative_blankKey() {
    given()
      .when().delete("{key}", SPACE)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_BAD_REQUEST))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "errors[0].type", is("ConstraintViolationException"),
        "errors[0].code", is(VALIDATION_ERROR.getValue()),
        "errors[0].message", is("Validation failed"),
        "errors[0].parameters[0].key", is("deleteEntry.key"),
        "errors[0].parameters[0].value", is("Key must not be blank"),
        "total_records", is(1)
      );
  }
}
