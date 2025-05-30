package org.folio.ssp.support.extensions;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.vault.VaultContainer;
import org.testcontainers.vault.VaultLogLevel;

@Log4j2
public class VaultServerTestResource implements QuarkusTestResourceConfigurableLifecycleManager<EnableVault> {

  private static final String VAULT_IMAGE = "hashicorp/vault:1.13.3";

  private static Vault ACTIVE_VAULT;

  private VaultServerManager serverManager;

  @Override
  public Map<String, String> start() {
    serverManager.start();

    ACTIVE_VAULT = new Vault(serverManager.getVaultConfig());

    return Map.of(
      "SECRET_STORE_VAULT_TOKEN", serverManager.getVaultToken(),
      "SECRET_STORE_VAULT_ADDRESS", serverManager.getServerUrl()
    );
  }

  @Override
  public void stop() {
    ACTIVE_VAULT = null;
    serverManager.stop();
  }

  @Override
  public void init(EnableVault annotation) {
    serverManager = new VaultServerManager(annotation.initCommands(), annotation.logLevel());
  }

  @Override
  public void inject(TestInjector testInjector) {
    testInjector.injectIntoFields(
      new Vault(serverManager.getVaultConfig()),
      new TestInjector.AnnotatedAndMatchesType(InjectVault.class, Vault.class));

    testInjector.injectIntoFields(
      new VaultConnectionConfig(serverManager.getServerUrl(), serverManager.getVaultToken()),
      new TestInjector.AnnotatedAndMatchesType(InjectVaultConnectionConfig.class, VaultConnectionConfig.class));
  }

  static Vault getActiveVault() {
    if (ACTIVE_VAULT == null) {
      throw new IllegalStateException("Vault client isn't initialized");
    }

    return ACTIVE_VAULT;
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
        .withEnv(VAULT_LOG_LEVEL_ENV, logLevel.config)
        .withLogConsumer(new Log4j2LogConsumer(log));

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

  @RequiredArgsConstructor
  private static final class Log4j2LogConsumer extends BaseConsumer<Log4j2LogConsumer> {

    private final Logger logger;
    private final boolean separateOutputStreams;

    Log4j2LogConsumer(Logger log) {
      this(log, false);
    }

    @Override
    public void accept(OutputFrame outputFrame) {
      final OutputFrame.OutputType outputType = outputFrame.getType();
      final String utf8String = outputFrame.getUtf8StringWithoutLineEnding();

      switch (outputType) {
        case END:
          break;
        case STDOUT:
          if (separateOutputStreams) {
            logger.info("{}", utf8String);
          } else {
            logger.info("{}: {}", outputType, utf8String);
          }
          break;
        case STDERR:
          if (separateOutputStreams) {
            logger.error("{}", utf8String);
          } else {
            logger.info("{}: {}", outputType, utf8String);
          }
          break;
        default:
          throw new IllegalArgumentException("Unexpected outputType " + outputType);
      }
    }
  }
}
