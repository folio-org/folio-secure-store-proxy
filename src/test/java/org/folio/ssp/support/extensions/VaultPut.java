package org.folio.ssp.support.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(VaultPut.List.class)
public @interface VaultPut {

  /**
   * Path in Vault where the key-value pairs will be put.
   * The path should be in the format "secret/data/my-secret".
   * Example:
   * {@code
   * @VaultPut(
   *   path = "secret/my-secret",
   *   keyValues = {
   *     "username=admin",
   *     "password=secret"
   *   }
   * )}
   *
   * @return - path in Vault.
   */
  String path();

  /**
   * Key-value pairs to be put into Vault.
   * The key-value pairs should be in the format "key=value".
   * Each key-value pair should be placed into a separate string in the array.
   * Example:
   * {@code
   * @VaultPut(
   *   path = "secret/my-secret",
   *   keyValues = {
   *    "username=admin",
   *    "password=secret"
   *   }
   * )}
   *
   * @return - array of key-value pairs to be put into Vault.
   */
  String[] keyValues();

  @Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface List {
    VaultPut[] value();
  }
}
