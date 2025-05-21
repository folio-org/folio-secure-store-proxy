package org.folio.ssp.service;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.folio.tools.store.SecureStore;

@Log4j2
@ApplicationScoped
public class SecureStoreEntryService {

  private final SecureStore secureStore;
  private final Cache entryCache;

  public SecureStoreEntryService(Instance<SecureStore> secureStore, @CacheName(ENTRY_CACHE) Cache entryCache) {
    this.secureStore = secureStore.get();
    this.entryCache = entryCache;
  }

  @CacheResult(cacheName = ENTRY_CACHE)
  public Uni<String> get(String key) {
    return executeBlocking(getInternal(key));
  }

  public Uni<Void> put(String key, String value) {
    return executeBlocking(putInternal(key, value))
      .invoke(() -> {
        entryCache.as(CaffeineCache.class).put(key, completedFuture(value));
        log.debug("Cache entry updated by \"put\" method: key = {}, value = {}", key, value);
      });
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
      log.debug("Entry retrieved: key = {}, value = {}", key, value);

      return value;
    };
  }

  private static <T> Uni<T> executeBlocking(Supplier<T> supplier) {
    return Uni.createFrom().item(supplier)
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
  }
}
