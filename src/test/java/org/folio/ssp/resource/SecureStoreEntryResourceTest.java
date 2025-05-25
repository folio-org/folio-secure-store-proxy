package org.folio.ssp.resource;

import static io.restassured.RestAssured.given;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;
import static org.folio.ssp.support.TestUtils.await;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.folio.ssp.model.SecureStoreEntry;
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
  void getEntry_positive() {
    secureStore.set("testKey", "value");

    given()
      .when().get("testKey")
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
      .when().put("testKey")
      .then()
      .statusCode(204); // No content for successful PUT
  }
}
