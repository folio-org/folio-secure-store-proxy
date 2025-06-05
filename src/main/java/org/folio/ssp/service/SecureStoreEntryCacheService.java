package org.folio.ssp.service;

import static java.util.stream.Collectors.toSet;
import static org.folio.ssp.SecureStoreConstants.ENTRY_CACHE;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ApplicationScoped
public class SecureStoreEntryCacheService {

  private final Cache entryCache;

  public SecureStoreEntryCacheService(@CacheName(ENTRY_CACHE) Cache entryCache) {
    this.entryCache = entryCache;
  }

  public Uni<Set<String>> getAllCachedKeys() {
    return Uni.createFrom().item(() -> entryCache.as(CaffeineCache.class).keySet()
      .stream()
      .map(String.class::cast)
      .collect(toSet()));
  }

  public Uni<Void> invalidate(String key) {
    return entryCache.invalidate(key)
      .invoke(() -> log.info("Cache entry invalidated: key = {}", key));
  }

  public Uni<Void> invalidateAll() {
    return entryCache.invalidateAll()
      .invoke(() -> log.info("All cache entries invalidated"));
  }
}
