package org.folio.ssp.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.ssp.SecureStoreConstants.ROLE_SECRETS_CACHE_ADMIN;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.Set;
import org.folio.ssp.model.validation.constraints.NotBlankKey;
import org.folio.ssp.service.SecureStoreEntryCacheService;

@Path("/entry-cache")
@RolesAllowed(ROLE_SECRETS_CACHE_ADMIN)
public class SecureStoreEntryCacheResource {

  private final SecureStoreEntryCacheService cacheService;

  public SecureStoreEntryCacheResource(SecureStoreEntryCacheService cacheService) {
    this.cacheService = cacheService;
  }

  @GET
  @Produces(APPLICATION_JSON)
  public Uni<Set<String>> getAllEntryKeys() {
    return cacheService.getAllCachedKeys();
  }

  @DELETE
  @Path("{key}")
  public Uni<Void> invalidateEntry(@NotBlankKey String key) {
    return cacheService.invalidate(key);
  }

  @DELETE
  public Uni<Void> invalidateAllEntries() {
    return cacheService.invalidateAll();
  }
}
