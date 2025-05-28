package org.folio.ssp.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.folio.ssp.model.validation.constraints.NotBlankKey;
import org.folio.ssp.model.validation.constraints.NotBlankValue;

@Data
@AllArgsConstructor(staticName = "of")
@RegisterForReflection
public class SecureStoreEntry {

  @NotBlankKey
  private String key;
  @NotBlankValue
  private String value;
}
