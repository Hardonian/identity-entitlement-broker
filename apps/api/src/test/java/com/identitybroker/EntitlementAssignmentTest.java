package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class EntitlementAssignmentTest {

    private static String tenantId;
    private static String userId;
    private static String groupId;
    private static String productId;
    private static String entitlementId;
    private static String assignmentId;

    @Test
    public void testCreateProduct() {
        tenantId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Entitlement Tenant\",\"slug\":\"ent-tenant\"}")
                .when().post("/api/v1/tenants")
                .then().statusCode(201).extract().path("id");

        productId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Test Product\",\"slug\":\"test-product\",\"description\":\"A test product\"}")
                .when().post("/api/v1/products")
                .then()
                .statusCode(201)
                .body("name", equalTo("Test Product"))
                .body("slug", equalTo("test-product"))
                .extract().path("id");
    }

    @Test
    public void testCreateEntitlement() {
        if (productId == null) testCreateProduct();
        entitlementId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"productId\":\"" + productId + "\",\"name\":\"Test Feature\"," +
                        "\"slug\":\"test-feature\",\"type\":\"FEATURE\"}")
                .when().post("/api/v1/entitlements")
                .then()
                .statusCode(201)
                .body("name", equalTo("Test Feature"))
                .body("slug", equalTo("test-feature"))
                .extract().path("id");
    }

    @Test
    public void testAssignToUser() {
        if (entitlementId == null) testCreateEntitlement();

        // Create a SCIM user
        userId = given()
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .body("{\"userName\":\"entuser1\",\"nameGiven\":\"Ent\",\"nameFamily\":\"User\"}")
                .when().post("/scim/v2/Users")
                .then().statusCode(201).extract().path("id");

        assignmentId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"tenantId\":\"" + tenantId + "\",\"entitlementId\":\"" + entitlementId + "\"," +
                        "\"userId\":\"" + userId + "\"}")
                .when().post("/api/v1/assignments")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("userId", equalTo(userId))
                .body("active", equalTo(true))
                .extract().path("id");
    }

    @Test
    public void testAssignToGroup() {
        if (entitlementId == null) testCreateEntitlement();

        // Create a SCIM group
        groupId = given()
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .body("{\"displayName\":\"Entitlement Group\"}")
                .when().post("/scim/v2/Groups")
                .then().statusCode(201).extract().path("id");

        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"tenantId\":\"" + tenantId + "\",\"entitlementId\":\"" + entitlementId + "\"," +
                        "\"groupId\":\"" + groupId + "\"}")
                .when().post("/api/v1/assignments")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("groupId", equalTo(groupId));
    }

    @Test
    public void testEffectiveEntitlements() {
        if (userId == null) testAssignToUser();
        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .when().get("/api/v1/assignments/user/{userId}", userId)
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test
    public void testRevokeEntitlement() {
        if (assignmentId == null) testAssignToUser();
        given()
                .header("X-Actor-Id", "test-user")
                .when().delete("/api/v1/assignments/{id}", assignmentId)
                .then()
                .statusCode(204);
    }
}
