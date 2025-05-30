package org.folio.ssp.support.extensions;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import com.bettercloud.vault.VaultException;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Log4j2
public class VaultTestExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    var vaultPuts = getVaultPuts(context);
    if (isEmpty(vaultPuts)) {
      return;
    }

    var vault = VaultServerTestResource.getActiveVault();
    for (var vaultPut : vaultPuts) {
      var keyValuePairs = getKeyValuePairs(vaultPut);

      try {
        vault.logical().write(vaultPut.path(), keyValuePairs);
        log.debug("Data saved in Vault: path = {}, keyValues = {}", vaultPut.path(), keyValuePairs);
      } catch (VaultException e) {
        throw new RuntimeException("Failed to put data into Vault: path = " + vaultPut.path()
          + ", keyValues = " + keyValuePairs, e);
      }
    }
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    var vaultPuts = getVaultPuts(context);
    if (isEmpty(vaultPuts)) {
      return;
    }

    var vault = VaultServerTestResource.getActiveVault();
    for (var vaultPut : vaultPuts) {
      try {
        vault.logical().delete(vaultPut.path());
        log.debug("Data deleted from Vault: path = {}", vaultPut.path());
      } catch (VaultException e) {
        throw new RuntimeException("Failed to delete data from Vault: path = " + vaultPut.path(), e);
      }
    }
  }

  private static VaultPut[] getVaultPuts(ExtensionContext context) {
    return context.getTestMethod()
      .map(method -> method.getAnnotationsByType(VaultPut.class))
      .orElse(null);
  }

  private static Map<String, Object> getKeyValuePairs(VaultPut vaultPut) {
    if (isEmpty(vaultPut.keyValues())) {
      throw new IllegalArgumentException("VaultPut annotation must have at least one key-value pair");
    }

    return Arrays.stream(vaultPut.keyValues())
      .map(String::trim)
      .filter(kvp -> !kvp.isEmpty())
      .map(kvp -> kvp.split("=", 2))
      .filter(kvp -> kvp.length == 2)
      .collect(toMap(kvp -> kvp[0], kvp -> kvp[1]));
  }
}
