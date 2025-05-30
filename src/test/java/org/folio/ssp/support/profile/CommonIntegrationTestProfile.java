package org.folio.ssp.support.profile;

import static org.folio.ssp.support.TestConstants.VAULT_SECRET_ROOT;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class CommonIntegrationTestProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
      "secret-store.type", "VAULT",
      "secret-store.vault.secret-root", VAULT_SECRET_ROOT
    );
  }
}
