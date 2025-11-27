package org.folio.ssp.it;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.TestProfile;
import org.folio.ssp.support.extensions.EnableVault;
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
class SecureStoreProxyHealthCheckIT {

  @TestHTTPResource(value = "/admin/health", management = true)
  String healthEndpointUrl;

  @Test
  void healthCheck_positive() {
    given()
      .when().get(healthEndpointUrl)
      .then()
      .log().ifValidationFails()
      .assertThat()
      .statusCode(is(SC_OK))
      .contentType(containsString(APPLICATION_JSON))
      .body("status", equalTo("UP"));
  }
}
