package org.folio.ssp.resource;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;
import org.folio.ssp.model.SecureStoreEntry;

@Path("/entry-cache")
public class EntryCacheResource {

  @GET
  public Uni<List<SecureStoreEntry>> getAll() {
    return Uni.createFrom().item(() -> {
      // Simulate a database call
      return List.of(SecureStoreEntry.of("key1", "value1"), SecureStoreEntry.of("key2", "value2"));
    });
  }

  @DELETE
  @Path("{key}")
  public Uni<Void> invalidate(String key) {
    return Uni.createFrom().item(() -> {
      // Simulate a database call
      return null;
    });
  }

  @DELETE
  public Uni<Void> invalidateAll() {
    return Uni.createFrom().item(() -> {
      // Simulate a database call
      return null;
    });
  }
}
