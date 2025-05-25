package org.folio.ssp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;
import static org.folio.ssp.support.TestUtils.await;
import static org.mockito.Mockito.when;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.folio.ssp.configuration.ConfiguredSecureStore;
import org.folio.support.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@UnitTest
@QuarkusTest
class SecureStoreEntryServiceTest {

  @InjectMock @ConfiguredSecureStore SecureStore secureStore;
  @Inject @CacheName(ENTRY_CACHE) Cache entryCache;
  @Inject SecureStoreEntryService service;


  @AfterEach
  void tearDown() {
    await(entryCache.invalidateAll());
  }

  @Test
  @Order(1)
  void get_positive1() {
    when(secureStore.get("key1")).thenReturn("value1");

    var result = await(service.get("key1"));

    assertThat(result).isEqualTo("value1");
  }

  @Test
  @Order(2)
  void get_positive2() {
    when(secureStore.get("key2")).thenReturn("value2");
    
    var result = await(service.get("key2"));

    assertThat(result).isEqualTo("value2");
  }
}
