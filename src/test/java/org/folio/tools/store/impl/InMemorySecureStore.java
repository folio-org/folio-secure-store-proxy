package org.folio.tools.store.impl;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import org.folio.tools.store.SecureStore;

public final class InMemorySecureStore implements SecureStore {

  private final Map<String, String> data;

  private InMemorySecureStore(Map<String, String> data) {
    requireNonNull(data);
    this.data = data;
  }

  public static InMemorySecureStore empty() {
    return new InMemorySecureStore(new HashMap<>());
  }

  public static InMemorySecureStore from(Map<String, String> data) {
    return new InMemorySecureStore(new HashMap<>(data));
  }

  public Map<String, String> getData() {
    return data;
  }

  @Override
  public String get(String key) {
    return data.get(key);
  }

  @Override
  public String get(String clientId, String tenant, String username) {
    throw new UnsupportedOperationException("Deprecated method is not supported.");
  }

  @Override
  public void set(String key, String value) {
    data.put(key, value);
  }
}
