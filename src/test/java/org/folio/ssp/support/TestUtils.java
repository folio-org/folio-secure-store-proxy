package org.folio.ssp.support;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  public static <T> T await(Uni<T> uni) {
    return uni.await().indefinitely();
  }

  public static Optional<String> getCached(Cache cache, String key) throws InterruptedException, ExecutionException {
    var future = cache.as(CaffeineCache.class).getIfPresent(key);

    return future != null ? Optional.of((String) future.get()) : Optional.empty();
  }
}
