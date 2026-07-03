package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PolicyDecisionTest {

    private static String tenantId;
    private static String userId;

    @Test
    public void testAccessAllowed() {
        // Create tenant and user
        tenantId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Policy Tenant\",\"slug\":\"policy-tenant\"}")
                .when().post("/api/v1/tenants")
                .then().statusCode(201).extract().path("id");

        userId = given()
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .body("{\"userName\":\"policyuser1\"}")
                .when().post("/scim/v2/Users")
                .then().statusCode(201).extract().path("id");

        // Create product and entitlement, and assign to user
        String productId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Policy Product\",\"slug\":\"policy-product\"}")
                .when().post("/api/v1/products")
                .then().statusCode(201).extract().path("id");

        String entitlementId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"productId\":\"" + productId + "\",\"name\":\"Policy Access\"," +
                        "\"slug\":\"policy-access\"}")
                .when().post("/api/v1/entitlements")
                .then().statusCode(201).extract().path("id");

        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"tenantId\":\"" + tenantId + "\",\"entitlementId\":\"" + entitlementId + "\"," +
                        "\"userId\":\"" + userId + "\"}")
                .when().post("/api/v1/assignments")
                .then().statusCode(201);

        // Test decide with the user's entitlements
        // Since the user has entitlements, read actions should be allowed
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"tenantId\":\"" + tenantId + "\",\"action\":\"read:users\"," +
                        "\"subject\":{\"id\":\"" + userId + "\",\"type\":\"user\"}}")
                .when().post("/api/v1/policy/decide")
                .then()
                .statusCode(200)
                .body("allowed", equalTo(true));
    }

    @Test
    public void testAccessDenied() {
        if (tenantId == null) testAccessAllowed();

        // User with no entitlements -> should be denied for a non-read action
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"tenantId\":\"" + tenantId + "\",\"action\":\"delete:users\"," +
                        "\"subject\":{\"id\":\"nonexistent-user\",\"type\":\"user\"}}")
                .when().post("/api/v1/policy/decide")
                .then()
                .statusCode(200)
                .body("allowed", equalTo(false))
                .body("reason", notNullValue());
    }

    @Test
    public void testAdminCanManageTenant() {
        if (tenantId == null) testAccessAllowed();

        // Admin role should have full access
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"tenantId\":\"" + tenantId + "\",\"action\":\"manage:tenant\"," +
                        "\"subject\":{\"id\":\"admin-user\",\"type\":\"user\",\"roles\":[\"admin\"]}}")
                .when().post("/api/v1/policy/decide")
                .then()
                .statusCode(200)
                .body("allowed", equalTo(true));
    }
}
