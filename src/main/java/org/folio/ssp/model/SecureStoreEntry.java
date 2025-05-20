package org.folio.ssp.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
@RegisterForReflection
public class SecureStoreEntry {

  private String key;
  private String value;
}
