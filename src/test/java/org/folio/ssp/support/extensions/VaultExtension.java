package org.folio.ssp.support.extensions;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.testcontainers.vault.VaultContainer;
import org.testcontainers.vault.VaultLogLevel;

@Log4j2
public class VaultExtension implements QuarkusTestResourceConfigurableLifecycleManager<EnableVault> {

  private static final String VAULT_IMAGE = "hashicorp/vault:1.13.3";

  private VaultServerManager serverManager;

  @Override
  public Map<String, String> start() {
    serverManager.start();
    
    return Map.of(
      "SECRET_STORE_VAULT_TOKEN", serverManager.getVaultToken(),
      "SECRET_STORE_VAULT_ADDRESS", serverManager.getServerUrl()
    );
  }

  @Override
  public void stop() {
    serverManager.stop();
  }

  @Override
  public void init(EnableVault annotation) {
    serverManager = new VaultServerManager(annotation.initCommands(), annotation.logLevel());
  }

  @Override
  public void inject(TestInjector testInjector) {
    var vault = new Vault(serverManager.getVaultConfig());

    testInjector.injectIntoFields(
      vault,
      new TestInjector.AnnotatedAndMatchesType(InjectVault.class, Vault.class));

    testInjector.injectIntoFields(
      new VaultConnectionConfig(serverManager.getServerUrl(), serverManager.getVaultToken()),
      new TestInjector.AnnotatedAndMatchesType(InjectVaultConnectionConfig.class, VaultConnectionConfig.class));
  }

  private static final class VaultServerManager {

    private static final String VAULT_LOG_LEVEL_ENV = "VAULT_LOG_LEVEL";
    private static final String VAULT_TOKEN = "vault-root-token";

    private final String[] initCommands;
    private final VaultLogLevel logLevel;
    private VaultContainer<?> vaultContainer;

    VaultServerManager(String[] initCommands, VaultLogLevel logLevel) {
      this.initCommands = initCommands;
      this.logLevel = logLevel;
    }

    VaultContainer<?> getServer() {
      if (vaultContainer == null) {
        throw new IllegalStateException("Vault server isn't initialized");
      }
      return vaultContainer;
    }

    String getServerUrl() {
      return getServer().getHttpHostAddress();
    }

    VaultConfig getVaultConfig() {
      try {
        return new VaultConfig()
          .address(getServerUrl())
          .token(VAULT_TOKEN)
          .build();
      } catch (VaultException e) {
        throw new RuntimeException(e);
      }
    }

    String getVaultToken() {
      return VAULT_TOKEN;
    }

    void start() {
      if (vaultContainer != null) {
        log.debug("Vault server already started at: {}", getServerUrl());
        return;
      }

      vaultContainer = new VaultContainer<>(VAULT_IMAGE)
        .withVaultToken(VAULT_TOKEN)
        .withEnv(VAULT_LOG_LEVEL_ENV, logLevel.config);

      if (!ObjectUtils.isEmpty(initCommands)) {
        vaultContainer.withInitCommand(initCommands);
      }

      try {
        vaultContainer.start();
        log.info("Vault server started at: {}", getServerUrl());
      } catch (Exception e) {
        vaultContainer = null;
        throw new RuntimeException(e);
      }
    }

    void stop() {
      if (vaultContainer != null) {
        try {
          var url = getServerUrl();
          vaultContainer.stop();
          log.info("Vault server stopped at: {}", url);
        } finally {
          vaultContainer = null;
        }
      }
    }
  }
}
