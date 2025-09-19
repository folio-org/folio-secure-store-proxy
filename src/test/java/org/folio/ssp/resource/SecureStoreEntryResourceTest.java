package org.folio.ssp.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;
import static org.folio.ssp.model.error.ErrorCode.VALIDATION_ERROR;
import static org.folio.ssp.support.AssertionUtils.assertCached;
import static org.folio.ssp.support.AssertionUtils.assertNotCached;
import static org.folio.ssp.support.RestUtils.givenAdminClient;
import static org.folio.ssp.support.RestUtils.givenForbiddenUserClient;
import static org.folio.ssp.support.RestUtils.givenUnauthorizedUserClient;
import static org.folio.ssp.support.RestUtils.givenUserClient;
import static org.folio.ssp.support.TestConstants.KEY1;
import static org.folio.ssp.support.TestConstants.VALUE1;
import static org.folio.ssp.support.TestConstants.VALUE2;
import static org.folio.ssp.support.TestUtils.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import javax.net.ssl.SSLHandshakeException;
import org.folio.ssp.model.SecureStoreEntry;
import org.folio.ssp.model.error.ErrorCode;
import org.folio.ssp.support.profile.InMemorySecureStoreTestProfile;
import org.folio.support.types.UnitTest;
import org.folio.tools.store.impl.InMemorySecureStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
@QuarkusTest
@TestProfile(InMemorySecureStoreTestProfile.class)
class SecureStoreEntryResourceTest {

  @Inject InMemorySecureStore secureStore;
  @Inject @CacheName(ENTRY_CACHE) Cache entryCache;

  @TestHTTPEndpoint(SecureStoreEntryResource.class)
  @TestHTTPResource(tls = true)
  String sseResourceUrl;

  @AfterEach
  void tearDown() {
    secureStore.getData().clear();
    await(entryCache.invalidateAll());
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void getEntry_positive(RequestSpecification spec, @SuppressWarnings("unused") String client) throws Exception {
    secureStore.set(KEY1, VALUE1);

    spec.when().get(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "key", is(KEY1),
        "value", is(VALUE1));

    assertCached(entryCache, KEY1, VALUE1);
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void getEntry_negative_notFound(RequestSpecification spec, @SuppressWarnings("unused") String client) {
    spec.when().get(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NOT_FOUND))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "errors[0].type", is("SecretNotFoundException"),
        "errors[0].code", is(ErrorCode.NOT_FOUND_ERROR.getValue()),
        "errors[0].message", is("Entry not found: key = " + KEY1),
        "total_records", is(1));
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void getEntry_negative_blankKey(RequestSpecification spec, @SuppressWarnings("unused") String client) {
    spec.when().get(sseResourceUrl + "/{key}", SPACE)
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
        "total_records", is(1));
  }

  @Test
  void getEntry_negative_forbiddenUser() {
    givenForbiddenUserClient()
      .when().get(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_FORBIDDEN));
  }

  @Test
  void getEntry_negative_unauthorizedUser() {
    assertThatThrownBy(() -> givenUnauthorizedUserClient().when().get(sseResourceUrl + "/{key}", KEY1))
      .isInstanceOf(SSLHandshakeException.class)
      .hasMessageContaining("Received fatal alert: bad_certificate");
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void setEntry_positive_entryCreated(RequestSpecification spec, @SuppressWarnings("unused") String client)
    throws Exception {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE1);

    spec
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.get(KEY1)).isEqualTo(VALUE1);
    assertCached(entryCache, KEY1, VALUE1);
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void setEntry_positive_entryUpdated(RequestSpecification spec, @SuppressWarnings("unused") String client)
    throws Exception {
    secureStore.set(KEY1, VALUE1);

    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE2);

    spec
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.get(KEY1)).isEqualTo(VALUE2);
    assertCached(entryCache, KEY1, VALUE2);
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void setEntry_negative_blankKey(RequestSpecification spec, @SuppressWarnings("unused") String client) {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE1);

    spec
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put(sseResourceUrl + "/{key}", SPACE)
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

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void setEntry_negative_blankValue(RequestSpecification spec, @SuppressWarnings("unused") String client) {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, null);

    spec
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put(sseResourceUrl + "/{key}", KEY1)
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
  void setEntry_negative_forbiddenUser() {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE1);

    givenForbiddenUserClient()
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_FORBIDDEN));
  }

  @Test
  void setEntry_negative_unauthorizedUser() {
    SecureStoreEntry entry = SecureStoreEntry.of(KEY1, VALUE1);

    assertThatThrownBy(() ->
      givenUnauthorizedUserClient().contentType(ContentType.JSON).body(entry)
        .when().put(sseResourceUrl + "/{key}", KEY1))
      .isInstanceOf(SSLHandshakeException.class)
      .hasMessageContaining("Received fatal alert: bad_certificate");
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void deleteEntry_positive(RequestSpecification spec, @SuppressWarnings("unused") String client)
    throws Exception {
    secureStore.set(KEY1, VALUE1);

    spec.when().delete(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.getData().get(KEY1)).isNull();
    assertNotCached(entryCache, KEY1);
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void deleteEntry_positive_notStoredEntry(RequestSpecification spec, @SuppressWarnings("unused") String client)
    throws Exception {
    spec.when().delete(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));

    assertThat(secureStore.getData().get(KEY1)).isNull();
    assertNotCached(entryCache, KEY1);
  }

  @ParameterizedTest(name = "{index} authorized client: {1}")
  @MethodSource("authorizedClientProvider")
  void deleteEntry_negative_blankKey(RequestSpecification spec, @SuppressWarnings("unused") String client) {
    spec.when().delete(sseResourceUrl + "/{key}", SPACE)
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
        "total_records", is(1));
  }

  @Test
  void deleteEntry_negative_forbiddenUser() {
    givenForbiddenUserClient()
      .when().delete(sseResourceUrl + "/{key}", KEY1)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_FORBIDDEN));
  }

  @Test
  void deleteEntry_negative_unauthorizedUser() {
    assertThatThrownBy(() -> givenUnauthorizedUserClient().when().delete(sseResourceUrl + "/{key}", KEY1))
      .isInstanceOf(SSLHandshakeException.class)
      .hasMessageContaining("Received fatal alert: bad_certificate");
  }

  private static Stream<Arguments> authorizedClientProvider() {
    return Stream.of(
      Arguments.of(givenUserClient(), "FSSP User"),
      Arguments.of(givenAdminClient(), "FSSP Admin")
    );
  }
}
