package org.folio.ssp.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import lombok.AllArgsConstructor;
import org.folio.ssp.model.SecureStoreEntry;
import org.folio.ssp.service.SecureStoreEntryService;
import org.jboss.resteasy.reactive.RestPath;

@Path("/entries/{key}")
@AllArgsConstructor
public class SecureStoreEntryResource {

  @Inject
  SecureStoreEntryService entryService;

  @GET
  @Produces(APPLICATION_JSON)
  public Uni<SecureStoreEntry> getEntry(@NotBlank String key) {
    return entryService.get(key).map(s -> SecureStoreEntry.of(key, s));
  }

  @PUT
  @Consumes(APPLICATION_JSON)
  public Uni<Void> setEntry(@RestPath @NotBlank String key, @Valid SecureStoreEntry entry) {
    return entryService.put(key, entry.getValue());
  }

  @DELETE
  public Uni<Void> deleteEntry(@NotBlank String key) {
    return entryService.delete(key);
  }
}
