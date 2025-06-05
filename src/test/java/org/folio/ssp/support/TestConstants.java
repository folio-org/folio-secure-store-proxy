package org.folio.ssp.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestConstants {

  public static final String VAULT_SECRET_ROOT = "test-secrets";

  public static final String KEY1 = "key1";
  public static final String VALUE1 = "value1";
  public static final String KEY2 = "key2";
  public static final String VALUE2 = "value2";

  public static final String SECRET_PATH_TENANT1 = VAULT_SECRET_ROOT + "/folio/tenant1";
  public static final String SECRET_PATH_TENANT2 = VAULT_SECRET_ROOT + "/folio/tenant2";
  public static final String KEY_PREFIX_TENANT1 = "folio_tenant1_";
  public static final String KEY_PREFIX_TENANT2 = "folio_tenant2_";

  @Getter
  public enum Tenants {
    TENANT1(SECRET_PATH_TENANT1, KEY_PREFIX_TENANT1),
    TENANT2(SECRET_PATH_TENANT2, KEY_PREFIX_TENANT2);

    private final String secretPath;
    private final String keyPrefix;

    Tenants(String secretPath, String keyPrefix) {
      this.secretPath = secretPath;
      this.keyPrefix = keyPrefix;
    }
  }
}
