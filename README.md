# folio-secure-store-proxy

Copyright (C) 2025 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of Contents

- [Overview](#overview)
- [API Description](#api-description)
- [Configuration](#configuration)
  - [General Application Configuration](#general-application-configuration)
  - [SSL Configuration (FIPS profile)](#ssl-configuration-fips-profile)
  - [Logging Configuration](#logging-configuration)
  - [Secret Store Configuration](#secret-store-configuration)
    - [AWS SSM Specific](#aws-ssm-specific)
    - [Vault Specific](#vault-specific)
  - [Cache Configuration](#cache-configuration)
- [Building](#building)
  - [Über-JAR](#uber-jar)
  - [Native Executable](#native-executable)
  - [Building FIPS compatible image](#building-fips-compatible-image)
- [Running](#running)
  - [Running in Development Mode](#running-in-development-mode)
  - [Running the Packaged Application](#running-the-packaged-application)
  - [Running with Docker](#running-with-docker)

## Overview

`folio-secure-store-proxy` is a Quarkus-based service designed to act as a secure proxy for accessing secrets stored in various backend systems like AWS Systems Manager Parameter Store (SSM) or HashiCorp Vault. It is intended for use within the FOLIO platform to centralize and secure access to sensitive configuration data. This project uses [Quarkus](https://quarkus.io/).

## API Description

The module exposes a RESTful API for retrieving secrets. The complete OpenAPI specification, detailing all endpoints, request/response schemas, and methods, is available at the following path of the running application:

    /q/openapi

## Configuration

The application is configured through properties in the `application.properties` file. Many of these properties can be overridden by environment variables. For Quarkus properties (e.g., `quarkus.http.port`), the corresponding environment variable typically follows the pattern `QUARKUS_HTTP_PORT` (uppercase, dots replaced by underscores). Properties specifically defined with `${ENV_VAR}` or `${ENV_VAR:default_value}` in `application.properties` are directly mapped to environment variables.

### General Application Configuration

| Property Key                          | Description                                 | Default Value              |
|---------------------------------------|---------------------------------------------|----------------------------|
| `quarkus.application.name`            | The name of the application.                | `folio-secure-store-proxy` |
| `quarkus.http.port`                   | The HTTP port the application listens on.   | `8081`                     |
| `quarkus.http.ssl-port`               | The HTTPS port the application listens on.  | `8443`                     |
| `quarkus.rest.path`                   | The base path for the REST API.             | `secure-store`             |
| `quarkus.smallrye-health.root-path`   | The path for health check endpoints.        | `/admin/health`            |
| `quarkus.security.security-providers` | Security providers used by the application. | `SunRsaSign,SunJCE`        |

### SSL Configuration (FIPS profile)
These properties are prefixed with `%fips.` in `application.properties`, meaning they apply when the `fips` Quarkus profile is active. The values are set via environment variables.

| Property in `application.properties`                        | Environment Variable            | Description                                        |
|-------------------------------------------------------------|---------------------------------|----------------------------------------------------|
| `%fips.quarkus.http.ssl.certificate.key-store-file`         | `SSP_TLS_KEYSTORE_PATH`         | Path to the SSL keystore file.                     |
| `%fips.quarkus.http.ssl.certificate.key-store-password`     | `SSP_TLS_KEYSTORE_PASSWORD`     | Password for the SSL keystore.                     |
| `%fips.quarkus.http.ssl.certificate.key-store-password-key` | `SSP_TLS_KEYSTORE_KEY_PASSWORD` | Password for the key within the SSL keystore.      |
| `%fips.quarkus.http.ssl.certificate.key-store-file-type`    | `SSP_TLS_KEY_STORE_FILE_TYPE`   | Type of the SSL keystore file (e.g., JKS, PKCS12). |
| `%fips.quarkus.http.ssl.certificate.key-store-provider`     | `SSP_TLS_KEY_STORE_PROVIDER`    | Provider for the SSL keystore.                     |

### Logging Configuration

| Property in `application.properties`         | Environment Variable | Description                                    | Default Value |
|----------------------------------------------|----------------------|------------------------------------------------|---------------|
| `quarkus.log.level`                          | `ROOT_LOG_LEVEL`     | Root logging level for the application.        | `INFO`        |
| `quarkus.log.category."org.folio.ssp".level` | `SSP_LOG_LEVEL`      | Logging level for the `org.folio.ssp` package. | `INFO`        |

### Secret Store Configuration

| Property in `application.properties` | Environment Variable | Description                                                          | Default Value |
|--------------------------------------|----------------------|----------------------------------------------------------------------|---------------|
| `secret-store.type`                  | `SECRET_STORE_TYPE`  | Type of secret store to use (e.g., `EPHEMERAL`, `AWS_SSM`, `VAULT`). | `EPHEMERAL`   |

#### AWS SSM Specific
These settings apply if `secret-store.type` is configured to use AWS SSM.

| Property in `application.properties`            | Environment Variable                            | Description                                                                      | Default Value (if any) |
|-------------------------------------------------|-------------------------------------------------|----------------------------------------------------------------------------------|------------------------|
| `secret-store.aws-ssm.region`                   | `SECRET_STORE_AWS_SSM_REGION`                   | AWS region for the SSM service.                                                  | (none)                 |
| `secret-store.aws-ssm.use-iam`                  | `SECRET_STORE_AWS_SSM_USE_IAM`                  | Whether to use IAM roles for authentication.                                     | `true`                 |
| `secret-store.aws-ssm.ecs-credentials-endpoint` | `SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_ENDPOINT` | ECS container credentials relative URI.                                          | (none)                 |
| `secret-store.aws-ssm.ecs-credentials-path`     | `SECRET_STORE_AWS_SSM_ECS_CREDENTIALS_PATH`     | ECS container credentials absolute path.                                         | (none)                 |
| `secret-store.aws-ssm.fips-enabled`             | `SECRET_STORE_AWS_SSM_FIPS_ENABLED`             | Configure whether the AWS SDK should use the AWS fips endpoints.                 | `false`                |
| `secret-store.aws-ssm.trust-store-path`         | `SECRET_STORE_AWS_SSM_TRUSTSTORE_PATH`          | Truststore file relative path (should start from a leading slash) for FIPS mode. | (none)                 |
| `secret-store.aws-ssm.trust-store-password`     | `SECRET_STORE_AWS_SSM_TRUSTSTORE_PASSWORD`      | Truststore password for FIPS mode.                                               | (none)                 |
| `secret-store.aws-ssm.trust-store-file-type`    | `SECRET_STORE_AWS_SSM_TRUSTSTORE_FILE_TYPE`     | Truststore file type.                                                            | (none)                 |

### Vault Specific
These settings apply if `secret-store.type` is configured to use Vault.

| Property in `application.properties`      | Environment Variable                      | Description                                                                         | Default Value (if any) |
|-------------------------------------------|-------------------------------------------|-------------------------------------------------------------------------------------|------------------------|
| `secret-store.vault.token`                | `SECRET_STORE_VAULT_TOKEN`                | token for accessing vault, may be a root token                                      | -                      |
| `secret-store.vault.address`              | `SECRET_STORE_VAULT_ADDRESS`              | the address of your vault                                                           | -                      |
| `secret-store.vault.enable-ssl`           | `SECRET_STORE_VAULT_ENABLE_SSL`           | whether or not to use SSL                                                           | false                  |
| `secret-store.vault.pem-file-path`        | `SECRET_STORE_VAULT_PEM_FILE_PATH`        | the path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding    | -                      |
| `secret-store.vault.keystore-password`    | `SECRET_STORE_VAULT_KEYSTORE_PASSWORD`    | the password used to access the JKS keystore (optional)                             | -                      |
| `secret-store.vault.keystore-file-path`   | `SECRET_STORE_VAULT_KEYSTORE_FILE_PATH`   | the path to a JKS keystore file containing a client cert and private key            | -                      |
| `secret-store.vault.truststore-file-path` | `SECRET_STORE_VAULT_TRUSTSTORE_FILE_PATH` | the path to a JKS truststore file containing Vault server certs that can be trusted | -                      |

### Cache Configuration
| Property Key                                              | Description                                                  | Default Value |
|-----------------------------------------------------------|--------------------------------------------------------------|---------------|
| `quarkus.cache.caffeine."entry-cache".initial-capacity`   | Initial capacity of the entry cache.                         | `20`          |
| `quarkus.cache.caffeine."entry-cache".maximum-size`       | Maximum size of the entry cache.                             | `500`         |
| `quarkus.cache.caffeine."entry-cache".expire-after-write` | Fixed duration to keep entry in the cache after its creation | `24h`         |

## Building

This module is built with Apache Maven.

To build the module, which includes compiling code, running tests, and packaging the application, execute:
```shell script
./mvnw clean package
```
This command packages the application as a "fast JAR". The main JAR file (`quarkus-run.jar`) and its dependencies (`lib/` directory) will be located in the `target/quarkus-app/` directory.

### Über-JAR
To build an _über-jar_ (a single executable JAR containing all dependencies), run:
```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```
The resulting JAR (e.g., `folio-secure-store-proxy-1.0.0-SNAPSHOT-runner.jar`) will be in the `target/` directory.

### Native Executable
You can create a native executable using GraalVM for improved startup time and reduced memory usage.

If GraalVM is installed and configured locally:
```shell script
./mvnw package -Dnative
```
If you do not have GraalVM installed, you can build the native executable within a Docker container:
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```
The native executable (e.g., `folio-secure-store-proxy-1.0.0-SNAPSHOT-runner`) will be created in the `target/` directory.
For more detailed information on building native executables with Quarkus, refer to the [Quarkus Maven tooling guide](https://quarkus.io/guides/maven-tooling).

### Building FIPS compatible image

```shell
export QUARKUS_SECURITY_SECURITY_PROVIDERS=BCFIPSJSSE
mvn clean -Pfips install
docker build -f docker/Dockerfile.fips -t {{image-tag}}:{{image-version}}
```
## Running

### Running in Development Mode
To run the application in development mode, which enables live coding and hot reloading:
```shell script
./mvnw quarkus:dev
```
The application will typically be accessible at `http://localhost:8081` (or the port configured via `quarkus.http.port`).
The Quarkus Dev UI is available at `http://localhost:8080/q/dev/` during development mode.

### Running the Packaged Application

#### Standard JAR (Fast JAR)
After building with `./mvnw package`, run the application using:
```shell script
java -jar target/quarkus-app/quarkus-run.jar
```

#### Über-JAR
If you built an _über-jar_:
```shell script
java -jar target/folio-secure-store-proxy-1.0.0-SNAPSHOT-runner.jar
```
(Ensure the JAR name matches the one generated in your `target/` directory).

#### Native Executable
If you built a native executable:
```shell script
./target/folio-secure-store-proxy-1.0.0-SNAPSHOT-runner
```
(Ensure the executable name matches the one generated in your `target/` directory).

### Running with Docker
This project includes several Dockerfiles in the `docker/` directory, allowing you to build and run the application in containers.

- **JVM Mode (`docker/Dockerfile.jvm`):**
  Uses the standard JVM to run the application (from the fast-jar build).
  To build the image:
  ```shell script
  docker build -f docker/Dockerfile.jvm -t folio-secure-store-proxy-jvm .
  ```
  To run the container (example):
  ```shell script
  docker run -e QUARKUS_HTTP_PORT=8081 -p 8081:8081 folio-secure-store-proxy-jvm
  ```

- **Native Mode (`docker/Dockerfile.native`):**
  Uses the native executable built with GraalVM.
  First, ensure the native executable is built (e.g., `./mvnw package -Dnative`). Then, build the image:
  ```shell script
  docker build -f docker/Dockerfile.native -t folio-secure-store-proxy-native .
  ```
  To run the container (example):
  ```shell script
  docker run -e QUARKUS_HTTP_PORT=8081 -p 8081:8081 folio-secure-store-proxy-native
  ```
  A `docker/Dockerfile.native-micro` is also available, which uses a distroless base image for an even smaller native container.

- **Legacy JAR Mode (`docker/Dockerfile.legacy-jar`):**
  For running as a traditional "legacy" JAR.
  First, build the legacy JAR: `./mvnw package -Dquarkus.package.type=legacy-jar`. Then, build the image:
  ```shell script
  docker build -f docker/Dockerfile.legacy-jar -t folio-secure-store-proxy-legacy-jar .
  ```
  To run the container (example):
  ```shell script
  docker run -e QUARKUS_HTTP_PORT=8081 -p 8081:8081 folio-secure-store-proxy-legacy-jar
  ```

**Note on Docker Configuration:** When running Docker containers, configure the application using environment variables as detailed in the [Configuration](#configuration) section. For instance, to set the secret store type, you might add `-e SECRET_STORE_TYPE=VAULT` to your `docker run` command. The root `Dockerfile` in the project may require review or adjustments to align with standard Quarkus packaging; it is generally recommended to use the specific Dockerfiles within the `docker/` directory for clarity and reliability.
