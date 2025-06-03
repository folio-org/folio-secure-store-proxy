package org.folio.ssp.it;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.ssp.model.error.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.ssp.model.error.ErrorCode.VALIDATION_ERROR;
import static org.folio.ssp.support.TestConstants.KEY1;
import static org.folio.ssp.support.TestConstants.KEY2;
import static org.folio.ssp.support.TestConstants.SECRET_PATH_TENANT1;
import static org.folio.ssp.support.TestConstants.SECRET_PATH_TENANT2;
import static org.folio.ssp.support.TestConstants.Tenants.TENANT1;
import static org.folio.ssp.support.TestConstants.Tenants.TENANT2;
import static org.folio.ssp.support.TestConstants.VALUE1;
import static org.folio.ssp.support.TestConstants.VALUE2;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.TestProfile;
import org.folio.ssp.model.SecureStoreEntry;
import org.folio.ssp.resource.SecureStoreEntryResource;
import org.folio.ssp.support.TestConstants.Tenants;
import org.folio.ssp.support.extensions.EnableVault;
import org.folio.ssp.support.extensions.InjectVault;
import org.folio.ssp.support.extensions.InjectVaultConnectionConfig;
import org.folio.ssp.support.extensions.VaultConnectionConfig;
import org.folio.ssp.support.extensions.VaultPut;
import org.folio.ssp.support.extensions.VaultTestExtension;
import org.folio.ssp.support.profile.CommonIntegrationTestProfile;
import org.folio.support.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@IntegrationTest
@TestProfile(CommonIntegrationTestProfile.class)
@EnableVault(initCommands = {
  "secrets enable -path=test-secrets -description='Eureka testing secrets' kv-v2", // enable kv-v2 secrets engine
  "write test-secrets/config max_versions=1 delete_version_after=0s cas_required=false" // configure the engine
})
@ExtendWith(VaultTestExtension.class)
class SecureStoreProxyIT {

  @InjectVaultConnectionConfig VaultConnectionConfig vaultConnectionConfig;
  @InjectVault Vault vault;

  @TestHTTPEndpoint(SecureStoreEntryResource.class)
  @TestHTTPResource
  String sseResourceUrl;

  @Test
  void test_vault() {
    given()
      .header("X-Vault-Token", vaultConnectionConfig.token())
      .when().get(vaultConnectionConfig.hostUrl() + "/v1/test-secrets/config")
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(is(APPLICATION_JSON))
      .body(
        "data.max_versions", is(1),
        "data.cas_required", is(false),
        "data.delete_version_after", is("0s")
      );
  }

  @Test
  @VaultPut(path = SECRET_PATH_TENANT1, keyValues = {
    KEY1 + "=" + VALUE1,
    KEY2 + "=" + VALUE2
  })
  @VaultPut(path = SECRET_PATH_TENANT2, keyValues = {
    KEY1 + "=" + VALUE1,
    KEY2 + "=" + VALUE2,
  })
  void getEntry_positive() {
    getEntryAndCheck(TENANT1, KEY1, VALUE1);
    getEntryAndCheck(TENANT1, KEY2, VALUE2);

    getEntryAndCheck(TENANT2, KEY1, VALUE1);
    getEntryAndCheck(TENANT2, KEY2, VALUE2);
  }

  @Test
  @VaultPut(path = SECRET_PATH_TENANT1, keyValues = {
    KEY1 + "=" + VALUE1
  })
  void getEntry_negative_notFound() {
    given()
      .when().get(sseResourceUrl + "/{key}", TENANT1.getKeyPrefix() + KEY2)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NOT_FOUND))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "errors[0].type", is("NotFoundException"),
        "errors[0].code", is(NOT_FOUND_ERROR.getValue()),
        "errors[0].message", is("Attribute: " + KEY2 + " not set for folio/tenant1"),
        "total_records", is(1)
      );
  }

  @Test
  void setEntry_positive_createEntry() throws VaultException {
    setEntryAndCheck(TENANT1, KEY1, VALUE1);
    getEntryAndCheck(TENANT1, KEY1, VALUE1);
    getFromVaultAndCheck(TENANT1, KEY1, VALUE1);

    setEntryAndCheck(TENANT2, KEY2, VALUE2);
    getEntryAndCheck(TENANT2, KEY2, VALUE2);
    getFromVaultAndCheck(TENANT2, KEY2, VALUE2);
  }

  @Test
  @VaultPut(path = SECRET_PATH_TENANT1, keyValues = {KEY1 + "=" + VALUE1})
  @VaultPut(path = SECRET_PATH_TENANT2, keyValues = {KEY2 + "=" + VALUE2})
  void setEntry_positive_updateEntry() throws VaultException {
    setEntryAndCheck(TENANT1, KEY1, VALUE2); // change VALUE1 to VALUE2
    getEntryAndCheck(TENANT1, KEY1, VALUE2);
    getFromVaultAndCheck(TENANT1, KEY1, VALUE2);

    setEntryAndCheck(TENANT2, KEY2, VALUE1); // change VALUE2 to VALUE1
    getEntryAndCheck(TENANT2, KEY2, VALUE1);
    getFromVaultAndCheck(TENANT2, KEY2, VALUE1);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void setEntry_negative_emptyValue(String value) {
    var requestBody = SecureStoreEntry.of(KEY1, value);

    given()
      .body(requestBody)
      .contentType(APPLICATION_JSON)
      .when().put(sseResourceUrl + "/{key}", TENANT1.getKeyPrefix() + KEY1)
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

  private void setEntryAndCheck(Tenants tenant, String key, String value) {
    var requestBody = SecureStoreEntry.of(key, value);

    given()
      .body(requestBody)
      .contentType(APPLICATION_JSON)
      .when().put(sseResourceUrl + "/{key}", tenant.getKeyPrefix() + key)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_NO_CONTENT));
  }

  private void getEntryAndCheck(Tenants tenant, String key, String expectedValue) {
    given()
      .when().get(sseResourceUrl + "/{key}", tenant.getKeyPrefix() + key)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "key", is(tenant.getKeyPrefix() + key),
        "value", is(expectedValue)
      );
  }

  private void getFromVaultAndCheck(Tenants tenant, String key, String expectedValue) throws VaultException {
    var actualValue = vault.logical().read(tenant.getSecretPath()).getData().get(key);
    assertThat(actualValue).isEqualTo(expectedValue);
  }
}
