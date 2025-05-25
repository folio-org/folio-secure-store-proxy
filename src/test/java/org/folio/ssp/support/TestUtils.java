package org.folio.ssp.support;

import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  public static <T> T await(Uni<T> uni) {
    return uni.await().indefinitely();
  }
}
