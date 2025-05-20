package org.folio.ssp;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@QuarkusTest
class GreetingResourceTest {
  @Test
  void testHelloEndpoint() {
    given()
      .when().get("/hello")
      .then()
      .statusCode(200)
      .body(is("Hello from Quarkus REST"));
  }
}
