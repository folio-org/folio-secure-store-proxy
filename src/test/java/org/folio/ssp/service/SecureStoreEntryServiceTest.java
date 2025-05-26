package org.folio.ssp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;
import static org.folio.ssp.support.TestUtils.await;
import static org.folio.ssp.support.TestUtils.getCached;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheException;
import io.quarkus.cache.CacheName;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.concurrent.ExecutionException;
import org.folio.ssp.configuration.Configured;
import org.folio.support.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;

@UnitTest
@QuarkusTest
class SecureStoreEntryServiceTest {

  private static final String KEY1 = "key1";
  private static final String VALUE1 = "value1";
  private static final String KEY2 = "KEY2";
  private static final String VALUE2 = "VALUE2";

  @InjectMock @Configured SecureStore secureStore;
  @Inject @CacheName(ENTRY_CACHE) Cache entryCache;
  @Inject SecureStoreEntryService service;

  @AfterEach
  void tearDown() {
    await(entryCache.invalidateAll());
    verifyNoMoreInteractions(secureStore);
  }

  @Test
  void get_positive() throws Exception {
    when(secureStore.get(KEY1)).thenReturn(VALUE1);

    var result = await(service.get(KEY1));

    assertThat(result).isEqualTo(VALUE1);
    assertCached(KEY1, VALUE1);

    verify(secureStore, times(1)).get(KEY1);
  }

  @Test
  void get_positive_returnCachedMultipleTimes() throws Exception {
    when(secureStore.get(KEY1)).thenReturn(VALUE1);

    for (int i = 0; i < 10; i++) {
      var result = await(service.get(KEY1));

      assertThat(result).isEqualTo(VALUE1);
      assertCached(KEY1, VALUE1);
    }

    verify(secureStore, times(1)).get(KEY1);
  }

  @Test
  void get_positive_returnDifferentValuesForDifferentKeys() throws Exception {
    when(secureStore.get(KEY1)).thenReturn(VALUE1);
    when(secureStore.get(KEY2)).thenReturn(VALUE2);

    var result1 = await(service.get(KEY1));
    var result2 = await(service.get(KEY2));

    assertThat(result1).isEqualTo(VALUE1);
    assertThat(result2).isEqualTo(VALUE2);

    assertCached(KEY1, VALUE1);
    assertCached(KEY2, VALUE2);

    verify(secureStore, times(1)).get(KEY1);
    verify(secureStore, times(1)).get(KEY2);
  }

  @ParameterizedTest
  @NullSource
  void get_negative_nullKey(String key) throws Exception {
    assertThatThrownBy(() -> await(service.get(key)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Null keys are not supported by the Quarkus application data cache");

    assertNotCached(KEY1);
    verify(secureStore, times(0)).get(key);
  }

  @ParameterizedTest
  @EmptySource
  void get_negative_blankKey(String key) throws Exception {
    assertThatThrownBy(() -> await(service.get(key)))
      .isInstanceOf(CacheException.class)
      .hasRootCauseInstanceOf(IllegalArgumentException.class)
      .hasRootCauseMessage("Key cannot be blank");

    assertNotCached(KEY1);
    verify(secureStore, times(0)).get(key);
  }

  @Test
  void get_negative_nullValue() throws Exception {
    when(secureStore.get(KEY1)).thenReturn(null);

    assertThatThrownBy(() -> await(service.get(KEY1)))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Entry not found: key = " + KEY1);

    assertNotCached(KEY1);
    verify(secureStore, times(1)).get(KEY1);
  }

  @Test
  void get_negative_notFound() throws Exception {
    when(secureStore.get(KEY1)).thenThrow(new NotFoundException("Entry not found: key = " + KEY1));

    assertThatThrownBy(() -> await(service.get(KEY1)))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Entry not found: key = " + KEY1);

    assertNotCached(KEY1);
    verify(secureStore, times(1)).get(KEY1);
  }

  @Test
  void put_positive() throws Exception {
    await(service.put(KEY1, VALUE1));

    assertCached(KEY1, VALUE1);

    verify(secureStore, times(1)).set(KEY1, VALUE1);
  }

  @Test
  void put_positive_overwriteExistingValue() throws Exception {
    when(secureStore.get(KEY1)).thenReturn(VALUE1);

    var result = await(service.get(KEY1));

    assertThat(result).isEqualTo(VALUE1);
    assertCached(KEY1, VALUE1);

    await(service.put(KEY1, VALUE2));
    assertCached(KEY1, VALUE2);

    verify(secureStore, times(1)).get(KEY1);
    verify(secureStore, times(1)).set(KEY1, VALUE2);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void put_negative_blankKey(String key) throws Exception {
    assertThatThrownBy(() -> await(service.put(key, VALUE1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Key cannot be blank");

    assertNotCached(KEY1);
    verify(secureStore, times(0)).set(key, VALUE1);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void put_negative_blankValue(String value) throws Exception {
    assertThatThrownBy(() -> await(service.put(KEY1, value)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value cannot be blank");

    assertNotCached(KEY1);
    verify(secureStore, times(0)).set(KEY1, value);
  }

  @Test
  void delete_positive() throws Exception {
    when(secureStore.get(KEY1)).thenReturn(VALUE1);

    var result = await(service.get(KEY1));
    assertThat(result).isEqualTo(VALUE1);
    assertCached(KEY1, VALUE1);

    await(service.delete(KEY1));
    assertNotCached(KEY1);

    verify(secureStore, times(1)).get(KEY1);
    verify(secureStore, times(1)).set(KEY1, null);
  }

  @Test
  void delete_positive_keyNotPresent() throws Exception {
    await(service.delete(KEY1));
    assertNotCached(KEY1);

    verify(secureStore, times(1)).set(KEY1, null);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void delete_negative_blankKey(String key) throws Exception {
    assertThatThrownBy(() -> await(service.delete(key)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Key cannot be blank");

    verify(secureStore, times(0)).set(key, null);
  }

  private void assertCached(String key, String expected) throws InterruptedException, ExecutionException {
    var cached = getCached(entryCache, key);
    assertThat(cached)
      .isPresent()
      .hasValue(expected);
  }

  private void assertNotCached(String key) throws InterruptedException, ExecutionException {
    var cached = getCached(entryCache, key);
    assertThat(cached).isNotPresent();
  }
}
