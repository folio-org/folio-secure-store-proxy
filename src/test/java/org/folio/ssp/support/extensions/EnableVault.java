package org.folio.ssp.support.extensions;

import io.quarkus.test.common.QuarkusTestResource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.testcontainers.vault.VaultLogLevel;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@QuarkusTestResource(value = VaultExtension.class, restrictToAnnotatedClass = true)
public @interface EnableVault {

  String[] initCommands();

  VaultLogLevel logLevel() default VaultLogLevel.Debug;
}
