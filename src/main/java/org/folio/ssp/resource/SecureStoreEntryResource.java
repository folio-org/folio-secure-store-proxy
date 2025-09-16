package org.folio.ssp.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.ssp.SecureStoreConstants.ROLE_SECRETS_USER;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.folio.ssp.model.SecureStoreEntry;
import org.folio.ssp.model.validation.constraints.NotBlankKey;
import org.folio.ssp.service.SecureStoreEntryService;
import org.jboss.resteasy.reactive.RestPath;

@Path("/entries")
@RolesAllowed(ROLE_SECRETS_USER)
public class SecureStoreEntryResource {

  private final SecureStoreEntryService entryService;

  public SecureStoreEntryResource(SecureStoreEntryService entryService) {
    this.entryService = entryService;
  }

  @GET
  @Path("{key}")
  @Produces(APPLICATION_JSON)
  public Uni<SecureStoreEntry> getEntry(@NotBlankKey String key) {
    return entryService.get(key).map(s -> SecureStoreEntry.of(key, s));
  }

  @PUT
  @Path("{key}")
  @Consumes(APPLICATION_JSON)
  public Uni<Void> setEntry(@RestPath @NotBlankKey String key, @Valid SecureStoreEntry entry) {
    return entryService.put(key, entry.getValue());
  }

  @DELETE
  @Path("{key}")
  public Uni<Void> deleteEntry(@NotBlankKey String key) {
    return entryService.delete(key);
  }
}
