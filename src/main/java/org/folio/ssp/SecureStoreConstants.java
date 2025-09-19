package org.folio.ssp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecureStoreConstants {

  public static final String ENTRY_CACHE = "entry-cache";

  public static final String ROLE_SECRETS_USER = "secrets-user";
  public static final String ROLE_SECRETS_CACHE_ADMIN = "secrets-cache-admin";
}
