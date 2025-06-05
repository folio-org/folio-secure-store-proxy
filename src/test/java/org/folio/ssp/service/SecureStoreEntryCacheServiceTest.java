package org.folio.ssp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;
import static org.folio.ssp.support.TestConstants.KEY1;
import static org.folio.ssp.support.TestConstants.KEY2;
import static org.folio.ssp.support.TestConstants.VALUE1;
import static org.folio.ssp.support.TestConstants.VALUE2;
import static org.folio.ssp.support.TestUtils.await;
import static org.folio.ssp.support.TestUtils.getCached;
import static org.folio.ssp.support.TestUtils.putInCache;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.concurrent.ExecutionException;
import org.folio.support.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

@UnitTest
@QuarkusTest
class SecureStoreEntryCacheServiceTest {

  @Inject @CacheName(ENTRY_CACHE) Cache entryCache;
  @Inject SecureStoreEntryCacheService cacheService;

  @AfterEach
  void tearDown() {
    await(entryCache.invalidateAll());
  }

  @Test
  void getAllCachedKeys_positive() {
    putInCache(entryCache, KEY1, VALUE1);
    putInCache(entryCache, KEY2, VALUE2);

    var cachedKeys = await(cacheService.getAllCachedKeys());
    assertThat(cachedKeys).containsExactlyInAnyOrder(KEY1, KEY2);
  }

  @Test
  void getAllCachedKeys_positive_emptyCache() {
    var cachedKeys = await(cacheService.getAllCachedKeys());
    assertThat(cachedKeys).isEmpty();
  }

  @Test
  void invalidate_positive() throws Exception {
    putInCache(entryCache, KEY1, VALUE1);

    await(cacheService.invalidate(KEY1));

    assertNotCached(KEY1);
  }

  @Test
  void invalidate_negative_keyNotPresent() throws Exception {
    await(cacheService.invalidate(KEY1));

    assertNotCached(KEY1);
  }

  @ParameterizedTest
  @NullSource
  @SuppressWarnings("java:S5778")
  void invalidate_negative_blankKey(String key) {
    assertThatThrownBy(() -> await(cacheService.invalidate(key)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Null keys are not supported by the Quarkus application data cache");
  }

  @Test
  void invalidateAll_positive() throws Exception {
    putInCache(entryCache, KEY1, VALUE1);
    putInCache(entryCache, KEY2, VALUE2);

    await(cacheService.invalidateAll());

    assertNotCached(KEY1);
    assertNotCached(KEY2);
  }

  @Test
  void invalidateAll_positive_emptyCache() {
    await(cacheService.invalidateAll());

    assertThat(entryCache.as(CaffeineCache.class).keySet()).isEmpty();
  }

  private void assertNotCached(String key) throws InterruptedException, ExecutionException {
    var cached = getCached(entryCache, key);
    assertThat(cached).isNotPresent();
  }
}
