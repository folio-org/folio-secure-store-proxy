package org.folio.ssp.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.folio.ssp.model.SecureStoreEntry;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@QuarkusTest
class EntryResourceTest {

  @Test
  void getEntry_positive() {
    given()
      .when().get("/secure-store/entries/testKey")
      .then()
      .statusCode(200)
      .body("key", is("testKey"))
      .body("value", is("value"));
  }

  @Test
  void setEntry_positive() {
    SecureStoreEntry entry = SecureStoreEntry.of("testKey", "newValue");

    given()
      .contentType(ContentType.JSON)
      .body(entry)
      .when().put("/secure-store/entries/testKey")
      .then()
      .statusCode(204); // No content for successful PUT
  }
}
