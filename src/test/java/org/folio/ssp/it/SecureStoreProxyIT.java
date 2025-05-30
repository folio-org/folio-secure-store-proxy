package org.folio.ssp.it;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.ssp.model.error.ErrorCode.NOT_FOUND_ERROR;
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
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.TestProfile;
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

@IntegrationTest
@TestProfile(CommonIntegrationTestProfile.class)
@EnableVault(initCommands = {
  "secrets enable -path=test-secrets -description='Eureka testing secrets' kv-v2", // enable kv-v2 secrets engine
  "write test-secrets/config max_versions=1 delete_version_after=0s cas_required=false" // configure the engine
})
@ExtendWith(VaultTestExtension.class)
public class SecureStoreProxyIT {

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

  private void getEntryAndCheck(Tenants tenant, String key, String value) {
    given()
      .when().get(sseResourceUrl + "/{key}", tenant.getKeyPrefix() + key)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(containsString(APPLICATION_JSON))
      .body(
        "key", is(tenant.getKeyPrefix() + key),
        "value", is(value)
      );
  }
}
