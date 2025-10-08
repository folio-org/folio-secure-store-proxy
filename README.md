# folio-secure-store-proxy

Copyright (C) 2025 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of Contents

- [Overview](#overview)
- [API Description](#api-description)
- [Role-Based Access Control (RBAC)](#role-based-access-control-rbac)
- [Configuration](#configuration)
  - [General Application Configuration](#general-application-configuration)
  - [Mutual TLS (mTLS) Configuration](#mutual-tls-mtls-configuration)
    - [What is Mutual TLS (mTLS)?](#what-is-mutual-tls-mtls)
    - [Purpose for folio-secure-store-proxy](#purpose-for-folio-secure-store-proxy)
    - [Configuration Properties](#configuration-properties)
    - [Certificate and Keystore/Truststore Management](#certificate-and-keystoretruststore-management)
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

## Role-Based Access Control (RBAC)

The `folio-secure-store-proxy` employs Role-Based Access Control (RBAC) to manage and protect access to its various endpoints. This ensures that only authorized clients, identified through their mTLS certificates, can perform specific operations.

Access roles are determined by the Common Name (CN) value extracted from the client's mTLS certificate. The following roles are defined and mapped based on the client certificate's CN:

*   **`secrets-user`**: This role grants access to secure store entry-related operations (e.g., fetching, storing, updating secrets).
*   **`secrets-cache-admin`**: This role grants access to secure store entry cache-related operations (e.g., clearing the cache).

**Client Certificate CN to Role Mapping:**

| Client Certificate CN | Assigned Roles                              | Description                                                                                                                                                                                                                                                                   |
|-----------------------|---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fssp-user`           | `secrets-user`                              | Clients with a certificate having `CN=fssp-user` are granted the `secrets-user` role, allowing them to perform basic secret management operations.                                                                                                                            |
| `fssp-admin`          | `secrets-user`, `secrets-cache-admin`       | Clients with a certificate having `CN=fssp-admin` are granted both `secrets-user` and `secrets-cache-admin` roles. This provides full access to secret management operations as well as cache administration.                                                                 |

This mechanism leverages the robust authentication provided by mTLS to securely assign roles to clients. For a deeper understanding of how mTLS integrates with RBAC in a Quarkus application, refer to the [Quarkus Security Authentication Mechanisms guide on Mutual TLS](https://quarkus.io/guides/security-authentication-mechanisms#mutual-tls).

## Configuration

The application is configured through properties in the `application.properties` file. Many of these properties can be overridden by environment variables. For Quarkus properties (e.g., `quarkus.http.port`), the corresponding environment variable typically follows the pattern `QUARKUS_HTTP_PORT` (uppercase, dots replaced by underscores). Properties specifically defined with `${ENV_VAR}` or `${ENV_VAR:default_value}` in `application.properties` are directly mapped to environment variables.

### General Application Configuration

| Property Key                          | Description                                 | Default Value              |
|---------------------------------------|---------------------------------------------|----------------------------|
| `quarkus.application.name`            | The name of the application.                | `folio-secure-store-proxy` |
| `quarkus.http.ssl-port`               | The HTTPS port the application listens on.  | `8443`                     |
| `quarkus.rest.path`                   | The base path for the REST API.             | `secure-store`             |
| `quarkus.smallrye-health.root-path`   | The path for health check endpoints.        | `/admin/health`            |
| `quarkus.security.security-providers` | Security providers used by the application. | `SunRsaSign,SunJCE`        |

### Mutual TLS (mTLS) Configuration

The `folio-secure-store-proxy` is designed to operate exclusively in Mutual TLS (mTLS) mode, ensuring robust, two-way authentication between the proxy server and its clients. This section details what mTLS is, why it's used here, and how to configure it, including examples for generating the necessary certificates and keystores/truststores.

#### What is Mutual TLS (mTLS)?

TLS (Transport Layer Security) is a cryptographic protocol designed to provide secure communication over a computer network. When you visit a website with `https://`, you're using TLS, where the server presents a certificate to the client for authentication (one-way authentication).

**Mutual TLS (mTLS)** takes this a step further by requiring *both* the client and the server to authenticate each other using certificates issued by a trusted Certificate Authority (CA). This means:
1.  The client verifies the server's identity.
2.  The server verifies the client's identity.

This mutual authentication significantly enhances security, ensuring that only trusted clients can communicate with the server, and vice versa.

#### Purpose for `folio-secure-store-proxy`

The `folio-secure-store-proxy` is a critical component for handling sensitive secrets. By enforcing mTLS, it ensures:
*   **Strong Client Authentication:** Only applications possessing a valid, trusted client certificate can connect to the proxy and request secrets. This prevents unauthorized access even if the network is compromised.
*   **Data Integrity and Confidentiality:** All communication between the client and the proxy is encrypted, protecting secrets in transit.

**Important:** The `folio-secure-store-proxy` **can only be run in mTLS mode**. It does not support unencrypted HTTP connections or regular one-way TLS connections.

#### Configuration Properties

The mTLS configuration for the `folio-secure-store-proxy` (acting as the server) is managed via `application.properties` (or environment variables). The following Quarkus-specific properties are essential:

| Property in `application.properties`                  | Environment Variable            | Description                                                                   |
|-------------------------------------------------------|---------------------------------|-------------------------------------------------------------------------------|
| `quarkus.http.ssl.certificate.key-store-file`         | `SSP_TLS_KEYSTORE_PATH`         | Path to the SSL keystore file.                                                |
| `quarkus.http.ssl.certificate.key-store-password`     | `SSP_TLS_KEYSTORE_PASSWORD`     | Password for the SSL keystore.                                                |
| `quarkus.http.ssl.certificate.key-store-password-key` | `SSP_TLS_KEYSTORE_KEY_PASSWORD` | Password for the key within the SSL keystore.                                 |
| `quarkus.http.ssl.certificate.key-store-file-type`    | `SSP_TLS_KEY_STORE_FILE_TYPE`   | Type of the SSL keystore file (e.g., JKS, PKCS12).                            |
| `quarkus.http.ssl.certificate.key-store-provider`     | `SSP_TLS_KEY_STORE_PROVIDER`    | Provider for the SSL keystore.                                                |
| `quarkus.http.ssl.certificate.trust-store-file`       | `SSP_TLS_TRUSTSTORE_PATH`       | Path to the SSL truststore file (used to verify client certificates in mTLS). |
| `quarkus.http.ssl.certificate.trust-store-password`   | `SSP_TLS_TRUSTSTORE_PASSWORD`   | Password for the SSL truststore.                                              |
| `quarkus.http.ssl.certificate.trust-store-file-type`  | `SSP_TLS_TRUSTSTORE_FILE_TYPE`  | Type of the SSL truststore file (e.g., JKS, PKCS12).                          |
| `quarkus.http.ssl.certificate.trust-store-provider`   | `SSP_TLS_TRUSTSTORE_PROVIDER`   | Provider for the SSL truststore.                                              |

**Note:** The client connecting to the `folio-secure-store-proxy` will also need its own keystore (containing its private key and certificate) and a truststore (containing the CA certificate that signed the server's certificate). The specific properties for client-side mTLS configuration will depend on the client application's framework.

#### Certificate and Keystore/Truststore Management

To set up mTLS with self-signed certificates, you'll need to create a private key and a self-signed certificate for both the server and the client. Each party's truststore will then contain the *other party's* public self-signed certificate to establish mutual trust. This section provides examples using `openssl` (for generating keys and certificates) and `keytool` (for managing Java keystores).

##### Prerequisites:

*   **`openssl`**: For generating keys and self-signed certificates.
*   **`keytool`**: A Java utility for managing keystores and truststores (comes with a Java Development Kit - JDK).

##### Step 1: Generate Server (fssp-server) Key and Self-Signed Certificate

First, create a dedicated directory for your certificate generation process.

```bash
# Create a directory for certificate generation (e.g., 'certs_gen')
mkdir certs_gen
cd certs_gen

# Create a subdirectory for server files
mkdir server
cd server

# Generate server private key
openssl genrsa -out fssp-server.key 2048

# Generate self-signed Server Certificate
# IMPORTANT: CN (Common Name) MUST match the hostname/IP address where the proxy will be accessed.
# Add Subject Alternative Names (SAN) if accessible via multiple hostnames/IPs.
openssl req -x509 -new -nodes -key fssp-server.key -sha256 -days 365 \
    -out fssp-server.crt -subj "/CN=fssp-server" \
    -reqexts SAN -config <(printf "[req]\ndistinguished_name=req_distinguished_name\n[req_distinguished_name]\n[SAN]\nsubjectAltName=DNS:localhost,IP:127.0.0.1")

echo "Server certificate and key created: fssp-server.key, fssp-server.crt"
```

##### Step 2: Generate Client (fssp-user) Key and Self-Signed Certificate

```bash
cd .. # Go back to certs_gen
mkdir client
cd client

# Generate client private key
openssl genrsa -out fssp-user.key 2048

# Generate self-signed Client Certificate
# CN (Common Name) identifies the client application.
openssl req -x509 -new -nodes -key fssp-user.key -sha256 -days 365 \
    -out fssp-user.crt -subj "/CN=fssp-user"

echo "Client certificate and key created: fssp-user.key, fssp-user.crt"
```

##### Step 3: Create Server's Keystore and Truststore

The server needs a keystore containing its own private key and certificate. Its truststore needs to contain the *client's* public certificate so it can authenticate incoming client connections.

```bash
cd ../server # Go back to certs_gen/server

# Create a PKCS12 keystore for the server (contains server's key and self-signed certificate)
# This will be used for quarkus.http.ssl.key-store-file
SERVER_KEYSTORE_PASS="server_keystore_password" # Change this to a strong password!
openssl pkcs12 -export -out server.p12 -name fssp-server -inkey fssp-server.key -in fssp-server.crt -passout pass:$SERVER_KEYSTORE_PASS

# Create a PKCS12 truststore for the server (contains the client's public certificate to trust clients)
# This will be used for quarkus.http.ssl.trust-store-file
SERVER_TRUSTSTORE_PASS="server_truststore_password" # Change this to a strong password!
openssl pkcs12 -export -nokeys -in ../client/fssp-user.crt -out server_truststore.p12 -passout pass:$SERVER_TRUSTSTORE_PASS -name fssp-user

echo "Server keystore (server.p12) and truststore (server_truststore.p12) created."
```

##### Step 4: Create Client's Keystore and Truststore

The client needs a keystore containing its own private key and certificate. Its truststore needs to contain the *server's* public certificate so it can authenticate the proxy server.

```bash
cd ../client # Go back to certs_gen/client

# Create a PKCS12 keystore for the client (contains client's key and self-signed certificate)
# This will be used by the client application to present its identity to the proxy
CLIENT_KEYSTORE_PASS="client_keystore_password" # Change this to a strong password!
openssl pkcs12 -export -out client.p12 -name fssp-user -inkey fssp-user.key -in fssp-user.crt -passout pass:$CLIENT_KEYSTORE_PASS

# Create a PKCS12 truststore for the client (contains the server's public certificate to trust the server)
# This will be used by the client application to verify the proxy's certificate
CLIENT_TRUSTSTORE_PASS="client_truststore_password" # Change this to a strong password!
openssl pkcs12 -export -nokeys -in ../server/fssp-server.crt -out client_truststore.p12 -passout pass:$CLIENT_TRUSTSTORE_PASS -name fssp-server

echo "Client keystore (client.p12) and truststore (client_truststore.p12) created."
```

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

#### Vault Specific
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
./mvn clean package
```
This command packages the application as a "fast JAR". The main JAR file (`quarkus-run.jar`) and its dependencies (`lib/` directory) will be located in the `target/quarkus-app/` directory.

### Über-JAR
To build an _über-jar_ (a single executable JAR containing all dependencies), run:
```shell script
./mvn package -Dquarkus.package.jar.type=uber-jar
```
The resulting JAR (e.g., `folio-secure-store-proxy-1.0.0-SNAPSHOT-runner.jar`) will be in the `target/` directory.

### Native Executable
You can create a native executable using GraalVM for improved startup time and reduced memory usage.

If GraalVM is installed and configured locally:
```shell script
./mvn package -Dnative
```
If you do not have GraalVM installed, you can build the native executable within a Docker container:
```shell script
./mvn package -Dnative -Dquarkus.native.container-build=true
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
./mvn quarkus:dev
```
The application will typically be accessible at `http://localhost:8081` (or the port configured via `quarkus.http.port`).
The Quarkus Dev UI is available at `http://localhost:8080/q/dev/` during development mode.

### Running the Packaged Application

#### Standard JAR (Fast JAR)
After building with `./mvn package`, run the application using:
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
  docker run -e QUARKUS_HTTPS_PORT=8443 -p 8443:8443 folio-secure-store-proxy-jvm
  ```

- **Native Mode (`docker/Dockerfile.native`):**
  Uses the native executable built with GraalVM.
  First, ensure the native executable is built (e.g., `./mvn package -Dnative`). Then, build the image:
  ```shell script
  docker build -f docker/Dockerfile.native -t folio-secure-store-proxy-native .
  ```
  To run the container (example):
  ```shell script
  docker run -e QUARKUS_HTTPS_PORT=8443 -p 8443:8443 folio-secure-store-proxy-native
  ```
  A `docker/Dockerfile.native-micro` is also available, which uses a distroless base image for an even smaller native container.

- **Legacy JAR Mode (`docker/Dockerfile.legacy-jar`):**
  For running as a traditional "legacy" JAR.
  First, build the legacy JAR: `./mvn package -Dquarkus.package.type=legacy-jar`. Then, build the image:
  ```shell script
  docker build -f docker/Dockerfile.legacy-jar -t folio-secure-store-proxy-legacy-jar .
  ```
  To run the container (example):
  ```shell script
  docker run -e QUARKUS_HTTPS_PORT=8443 -p 8443:8443 folio-secure-store-proxy-legacy-jar
  ```

**Note on Docker Configuration:** When running Docker containers, configure the application using environment variables as detailed in the [Configuration](#configuration) section. For instance, to set the secret store type, you might add `-e SECRET_STORE_TYPE=VAULT` to your `docker run` command. The root `Dockerfile` in the project may require review or adjustments to align with standard Quarkus packaging; it is generally recommended to use the specific Dockerfiles within the `docker/` directory for clarity and reliability.
