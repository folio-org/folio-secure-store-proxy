package org.folio.ssp.service;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.ssp.model.SecureStoreEntry;
import org.folio.tools.store.SecureStore;

@Log4j2
@ApplicationScoped
@RequiredArgsConstructor
public class EntryService {

  private final SecureStore secureStore;

  @Blocking
  public SecureStoreEntry get(String key) {
    return SecureStoreEntry.of(key, secureStore.get(key));
  }

  @Blocking
  public void put(String key, SecureStoreEntry entry) {
    secureStore.set(key, entry.getValue());
  }
}
