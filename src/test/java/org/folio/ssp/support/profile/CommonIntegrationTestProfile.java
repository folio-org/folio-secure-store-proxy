package org.folio.ssp.support.profile;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class CommonIntegrationTestProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
      "secret-store.type", "VAULT"
    );
  }
}
