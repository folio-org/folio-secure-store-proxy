package org.folio.ssp.support;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.specification.RequestSpecification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RestUtils {

  private static final RestAssuredConfig USER_CLIENT_CONFIG = userClientConfig();
  private static final RestAssuredConfig ADMIN_CLIENT_CONFIG = adminClientConfig();
  private static final RestAssuredConfig UNAUTH_USER_CLIENT_CONFIG = unauthUserClientConfig();
  private static final RestAssuredConfig FORBIDDEN_USER_CLIENT_CONFIG = forbiddenUserClientConfig();

  private static final String KEYSTORE_PASS = "supersecret";
  private static final String FSSP_CLIENT_TRUSTSTORE = "certificates/client/fssp-client-truststore.p12";
  private static final String PKCS_12 = "pkcs12";

  public static RequestSpecification givenUserClient() {
    return RestAssured.given().config(USER_CLIENT_CONFIG);
  }

  public static RequestSpecification givenUnauthorizedUserClient() {
    return RestAssured.given().config(UNAUTH_USER_CLIENT_CONFIG);
  }

  public static RequestSpecification givenForbiddenUserClient() {
    return RestAssured.given().config(FORBIDDEN_USER_CLIENT_CONFIG);
  }

  public static RequestSpecification givenAdminClient() {
    return RestAssured.given().config(ADMIN_CLIENT_CONFIG);
  }

  private static RestAssuredConfig userClientConfig() {
    return RestAssured.config().sslConfig(new SSLConfig()
      .keyStore("certificates/client/fssp-user-client-keystore.p12", KEYSTORE_PASS)
      .keystoreType(PKCS_12)
      .trustStore(FSSP_CLIENT_TRUSTSTORE, KEYSTORE_PASS)
      .trustStoreType(PKCS_12)
      .allowAllHostnames()
    );
  }

  private static RestAssuredConfig adminClientConfig() {
    return RestAssured.config().sslConfig(new SSLConfig()
      .keyStore("certificates/client/fssp-admin-client-keystore.p12", KEYSTORE_PASS)
      .keystoreType(PKCS_12)
      .trustStore(FSSP_CLIENT_TRUSTSTORE, KEYSTORE_PASS)
      .trustStoreType(PKCS_12)
      .allowAllHostnames()
    );
  }

  private static RestAssuredConfig unauthUserClientConfig() {
    return RestAssured.config().sslConfig(new SSLConfig()
      .keyStore("certificates/client/fssp-unauth-user-client-keystore.p12", KEYSTORE_PASS)
      .keystoreType(PKCS_12)
      .trustStore(FSSP_CLIENT_TRUSTSTORE, KEYSTORE_PASS)
      .trustStoreType(PKCS_12)
      .allowAllHostnames()
    );
  }

  private static RestAssuredConfig forbiddenUserClientConfig() {
    return RestAssured.config().sslConfig(new SSLConfig()
      .keyStore("certificates/client/fssp-forbidden-user-client-keystore.p12", KEYSTORE_PASS)
      .keystoreType(PKCS_12)
      .trustStore(FSSP_CLIENT_TRUSTSTORE, KEYSTORE_PASS)
      .trustStoreType(PKCS_12)
      .allowAllHostnames()
    );
  }
}
