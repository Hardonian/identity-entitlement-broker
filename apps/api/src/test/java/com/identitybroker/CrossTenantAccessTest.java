package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class CrossTenantAccessTest {

    private static String tenantAId;
    private static String tenantBId;
    private static String userInTenantAId;
    private static String idpInTenantAId;

    @Test
    public void testCrossTenantUserRead() {
        // Create Tenant A
        tenantAId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Tenant A\",\"slug\":\"tenant-a\"}")
                .when().post("/api/v1/tenants")
                .then().statusCode(201).extract().path("id");

        // Create Tenant B
        tenantBId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Tenant B\",\"slug\":\"tenant-b\"}")
                .when().post("/api/v1/tenants")
                .then().statusCode(201).extract().path("id");

        // Create user in Tenant A
        userInTenantAId = given()
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantAId)
                .header("X-Actor-Id", "test-user")
                .body("{\"userName\":\"userA1\",\"nameGiven\":\"User\",\"nameFamily\":\"A\"}")
                .when().post("/scim/v2/Users")
                .then().statusCode(201).extract().path("id");

        // Try to read user as Tenant B -> should fail with 403
        given()
                .header("X-Tenant-Id", tenantBId)
                .header("X-Actor-Id", "test-user")
                .when().get("/scim/v2/Users/{id}", userInTenantAId)
                .then()
                .statusCode(403);
    }

    @Test
    public void testCrossTenantIdpList() {
        if (tenantAId == null) testCrossTenantUserRead();

        // Create IdP in Tenant A
        idpInTenantAId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"providerType\":\"OIDC\",\"issuer\":\"https://tenant-a.auth.com\"}")
                .when().post("/api/v1/tenants/{tenantId}/idp", tenantAId)
                .then().statusCode(201).extract().path("id");

        // List IdP as Tenant B -> should be empty
        given()
                .header("X-Actor-Id", "test-user")
                .when().get("/api/v1/tenants/{tenantId}/idp", tenantBId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    public void testCrossTenantEntitlement() {
        if (tenantAId == null || userInTenantAId == null) testCrossTenantUserRead();

        // Create product
        String productId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Cross Tenant Product\",\"slug\":\"cross-product\"}")
                .when().post("/api/v1/products")
                .then().statusCode(201).extract().path("id");

        // Create entitlement
        String entitlementId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"productId\":\"" + productId + "\",\"name\":\"Cross Feature\"," +
                        "\"slug\":\"cross-feature\"}")
                .when().post("/api/v1/entitlements")
                .then().statusCode(201).extract().path("id");

        // Assign to user in Tenant A
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"tenantId\":\"" + tenantAId + "\",\"entitlementId\":\"" + entitlementId + "\"," +
                        "\"userId\":\"" + userInTenantAId + "\"}")
                .when().post("/api/v1/assignments")
                .then().statusCode(201);

        // Try to read user's entitlements as Tenant B -> should be empty since user is not in Tenant B
        given()
                .header("X-Tenant-Id", tenantBId)
                .header("X-Actor-Id", "test-user")
                .when().get("/api/v1/assignments/user/{userId}", userInTenantAId)
                .then()
                .statusCode(403);
    }
}
