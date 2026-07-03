package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class IdpRegistryValidationTest {

    private static String tenantId;
    private static String idpId;

    @Test
    public void testRegisterIdp() {
        // First create a tenant
        tenantId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"IdpTest Tenant\",\"slug\":\"idp-test-tenant\"}")
                .when().post("/api/v1/tenants")
                .then().statusCode(201).extract().path("id");

        // Register IdP
        idpId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"providerType\":\"OIDC\",\"issuer\":\"https://auth.test.com/oidc\"," +
                        "\"clientId\":\"test-client\",\"secretRef\":\"vault://test/secret\"}")
                .when().post("/api/v1/tenants/{tenantId}/idp", tenantId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("providerType", equalTo("OIDC"))
                .body("issuer", equalTo("https://auth.test.com/oidc"))
                .body("status", equalTo("ACTIVE"))
                .extract().path("id");
    }

    @Test
    public void testRegisterIdpInvalidType() {
        if (tenantId == null) {
            testRegisterIdp();
        }
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"providerType\":\"INVALID_TYPE\",\"issuer\":\"https://test.com\"}")
                .when().post("/api/v1/tenants/{tenantId}/idp", tenantId)
                .then()
                .statusCode(409); // ConflictException for invalid enum
    }

    @Test
    public void testRegisterIdpNoIssuer() {
        if (tenantId == null) {
            testRegisterIdp();
        }
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"providerType\":\"OIDC\"}")
                .when().post("/api/v1/tenants/{tenantId}/idp", tenantId)
                .then()
                .statusCode(400);
    }

    @Test
    public void testNoSecretLeak() {
        if (idpId == null) {
            testRegisterIdp();
        }
        String secretRef = given()
                .header("X-Actor-Id", "test-user")
                .when().get("/api/v1/tenants/{tenantId}/idp/{id}", tenantId, idpId)
                .then()
                .statusCode(200)
                .extract().path("secretRef");

        // Secret should be masked - last 4 chars visible, rest masked
        org.assertj.core.api.Assertions.assertThat(secretRef)
                .doesNotContain("vault://test/secret")
                .endsWith("cret");
    }
}
