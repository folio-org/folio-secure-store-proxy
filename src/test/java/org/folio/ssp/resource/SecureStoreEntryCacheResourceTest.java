package org.folio.ssp.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@QuarkusTest
class SecureStoreEntryCacheResourceTest {

  @Test
  void getAll_positive() {
    given()
      .when().get("/secure-store/entry-cache")
      .then()
      .statusCode(200)
      .body("", hasSize(2))
      .body("[0].key", is("key1"))
      .body("[0].value", is("value1"))
      .body("[1].key", is("key2"))
      .body("[1].value", is("value2"));
  }

  @Test
  void invalidate_positive() {
    given()
      .when().delete("/secure-store/entry-cache/key1")
      .then()
      .statusCode(204); // No content for successful DELETE
  }

  @Test
  void invalidateAll_positive() {
    given()
      .when().delete("/secure-store/entry-cache")
      .then()
      .statusCode(204); // No content for successful DELETE
  }
}
