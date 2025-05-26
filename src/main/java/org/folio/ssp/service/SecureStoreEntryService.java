package org.folio.ssp.service;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.folio.ssp.configuration.Configured;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.NotFoundException;

@Log4j2
@ApplicationScoped
public class SecureStoreEntryService {

  private final SecureStore secureStore;
  private final Cache entryCache;

  public SecureStoreEntryService(@Configured SecureStore secureStore, @CacheName(ENTRY_CACHE) Cache entryCache) {
    this.secureStore = secureStore;
    this.entryCache = entryCache;
  }

  @CacheResult(cacheName = ENTRY_CACHE)
  public Uni<String> get(String key) {
    validateKey(key);
    return executeBlocking(getInternal(key));
  }

  public Uni<Void> put(String key, String value) {
    validateKey(key);
    validateValue(value);

    return executeBlocking(putInternal(key, value))
      .invoke(() -> {
        entryCache.as(CaffeineCache.class).put(key, completedFuture(value));
        log.debug("Cache entry updated by \"put\" method: key = {}, value = {}", key, value);
      });
  }

  public Uni<Void> delete(String key) {
    validateKey(key);

    return executeBlocking(deleteInternal(key))
      .chain(() -> entryCache.invalidate(key)
        .invoke(() -> log.debug("Cache entry invalidated by \"delete\" method: key = {}", key))
      );
  }

  private Supplier<Void> putInternal(String key, String value) {
    return () -> {
      log.debug("Setting entry in secure store: key = {}, value = {}", key, value);
      secureStore.set(key, value);
      log.debug("Entry set: key = {}", key);

      return null;
    };
  }

  private Supplier<String> getInternal(String key) {
    return () -> {
      log.debug("Getting entry from secure store: key = {}", key);
      var value = secureStore.get(key);

      if (isEmpty(value)) {
        log.debug("Entry not found: key = {}", key);
        throw new NotFoundException("Entry not found: key = " + key);
      } else {
        log.debug("Entry retrieved: key = {}, value = {}", key, value);
        return value;
      }
    };
  }

  private Supplier<Object> deleteInternal(String key) {
    return () -> {
      log.debug("Deleting entry from secure store: key = {}", key);
      // TODO: replace with secureStore.delete(key) when implemented
      secureStore.set(key, null);
      log.debug("Entry deleted: key = {}", key);

      return null;
    };
  }

  private static void validateKey(String key) {
    if (isBlank(key)) {
      throw new IllegalArgumentException("Key cannot be blank");
    }
  }

  private static void validateValue(String value) {
    if (isBlank(value)) {
      throw new IllegalArgumentException("Value cannot be blank");
    }
  }

  private static <T> Uni<T> executeBlocking(Supplier<T> supplier) {
    return Uni.createFrom().item(supplier)
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
  }
}
