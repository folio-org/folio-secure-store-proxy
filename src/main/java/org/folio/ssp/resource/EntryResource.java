package org.folio.ssp.resource;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.folio.ssp.model.SecureStoreEntry;
import org.jboss.resteasy.reactive.RestPath;

@Path("/entries")
public class EntryResource {

  @GET
  @Path("{key}")
  @Produces(APPLICATION_JSON)
  public Uni<SecureStoreEntry> getEntry(String key) {
    return Uni.createFrom().item(() -> {
      // Simulate a database call
      return SecureStoreEntry.of(key, "value");
    });
  }

  @PUT
  @Path("{key}")
  @Consumes(APPLICATION_JSON)
  public Uni<Void> setEntry(@RestPath String key, SecureStoreEntry entry) {
    return Uni.createFrom().item(() -> {
      // Simulate a database call
      return null;
    });
  }
}
