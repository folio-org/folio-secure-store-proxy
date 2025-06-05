package org.folio.ssp.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.ssp.support.TestUtils.getCached;

import io.quarkus.cache.Cache;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssertionUtils {

  public static void assertCached(Cache cache, String key, String expected) throws Exception {
    var cached = getCached(cache, key);
    assertThat(cached)
      .isPresent()
      .hasValue(expected);
  }

  public static void assertNotCached(Cache cache, String key) throws Exception {
    var cached = getCached(cache, key);
    assertThat(cached).isNotPresent();
  }
}
