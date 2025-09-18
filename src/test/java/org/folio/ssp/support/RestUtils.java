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

  public static RequestSpecification givenUserClient() {
    return RestAssured.given().config(USER_CLIENT_CONFIG);
  }

  public static RequestSpecification givenAdminClient() {
    return RestAssured.given().config(ADMIN_CLIENT_CONFIG);
  }

  private static RestAssuredConfig userClientConfig() {
    return RestAssured.config().sslConfig(new SSLConfig()
      .keyStore("certificates/client/fssp-user-client-keystore.p12", "supersecret")
      .keystoreType("pkcs12")
      .trustStore("certificates/client/fssp-client-truststore.p12", "supersecret")
      .trustStoreType("pkcs12")
      .allowAllHostnames()
    );
  }

  private static RestAssuredConfig adminClientConfig() {
    return RestAssured.config().sslConfig(new SSLConfig()
      .keyStore("certificates/client/fssp-admin-client-keystore.p12", "supersecret")
      .keystoreType("pkcs12")
      .trustStore("certificates/client/fssp-client-truststore.p12", "supersecret")
      .trustStoreType("pkcs12")
      .allowAllHostnames()
    );
  }
}
