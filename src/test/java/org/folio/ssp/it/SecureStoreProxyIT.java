package org.folio.ssp.it;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

import com.bettercloud.vault.Vault;
import io.quarkus.test.junit.TestProfile;
import org.folio.ssp.support.extensions.EnableVault;
import org.folio.ssp.support.extensions.InjectVault;
import org.folio.ssp.support.extensions.InjectVaultConnectionConfig;
import org.folio.ssp.support.extensions.VaultConnectionConfig;
import org.folio.ssp.support.profile.CommonIntegrationTestProfile;
import org.folio.support.types.IntegrationTest;
import org.junit.jupiter.api.Test;

@IntegrationTest
@TestProfile(CommonIntegrationTestProfile.class)
@EnableVault(initCommands = {
  "secrets enable -path=folio-secrets -description='Eureka testing secrets' kv-v2", // enable kv-v2 secrets engine
  "write folio-secrets/config max_versions=1 delete_version_after=0s cas_required=false" // configure the engine
})
public class SecureStoreProxyIT {

  @InjectVaultConnectionConfig VaultConnectionConfig vaultConnectionConfig;
  @InjectVault Vault vault;

  @Test
  void test_vault() {
    given()
      .header("X-Vault-Token", vaultConnectionConfig.token())
      .when().get(vaultConnectionConfig.hostUrl() + "/v1/folio-secrets/config")
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
}
