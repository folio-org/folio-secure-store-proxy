package org.folio.ssp.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
@RegisterForReflection
public class SecureStoreEntry {

  @NotBlank(message = "Key cannot be blank")
  private String key;
  @NotBlank(message = "Value cannot be blank")
  private String value;
}
