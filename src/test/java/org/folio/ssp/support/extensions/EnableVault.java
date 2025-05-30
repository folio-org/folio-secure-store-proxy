package org.folio.ssp.support.extensions;

import io.quarkus.test.common.QuarkusTestResource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.testcontainers.vault.VaultLogLevel;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@QuarkusTestResource(value = VaultServerTestResource.class, restrictToAnnotatedClass = true)
public @interface EnableVault {

  /**
   * Commands to be executed on Vault server startup.
   * These commands are executed in the order they are defined.
   *
   * <p>
   * Example:
   * <pre>
   * {@code
   * initCommands = {
   *   "secrets enable -path=secret kv",
   *   "kv put secret/my-secret my-key=my-value"
   * }
   * }
   * </pre>
   *
   * @return array of commands to execute
   */
  String[] initCommands();

  /**
   * The log level for the Vault server.
   * Default is Info.
   *
   * @return the log level
   */
  VaultLogLevel logLevel() default VaultLogLevel.Info;
}
