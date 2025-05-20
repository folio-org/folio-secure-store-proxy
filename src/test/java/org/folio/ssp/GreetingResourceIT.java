package org.folio.ssp;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.folio.support.types.IntegrationTest;
import org.junit.jupiter.api.Test;

@IntegrationTest
@QuarkusIntegrationTest
class GreetingResourceIT {
  // Execute the same tests but in packaged mode.
  @Test
  void testHelloEndpoint() {
    given()
      .when().get("/hello")
      .then()
      .statusCode(200)
      .body(is("Hello from Quarkus REST"));
  }
}
