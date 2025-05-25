package org.folio.ssp.support.profile;

import io.quarkus.test.junit.QuarkusTestProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.Map;
import org.folio.tools.store.impl.InMemorySecureStore;

public class InMemorySecureStoreTestProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
      "secret-store.type", "IN_MEMORY"
    );
  }

  @Produces
  @ApplicationScoped
  public InMemorySecureStore inMemorySecureStore() {
    return InMemorySecureStore.empty();
  }
}
