package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ScimProvisioningTest {

    private static String tenantId;
    private static String userId;
    private static String groupId;

    @Test
    public void testCreateUser() {
        // Create a tenant
        tenantId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"SCIM Tenant\",\"slug\":\"scim-tenant\"}")
                .when().post("/api/v1/tenants")
                .then().statusCode(201).extract().path("id");

        // Create SCIM user
        userId = given()
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .body("{\"userName\":\"scimuser1\",\"nameGiven\":\"Scim\",\"nameFamily\":\"User\"," +
                        "\"email\":\"scim@test.com\"}")
                .when().post("/scim/v2/Users")
                .then()
                .statusCode(201)
                .body("schemas", hasItem("urn:ietf:params:scim:schemas:core:2.0:User"))
                .body("id", notNullValue())
                .body("userName", equalTo("scimuser1"))
                .body("meta.resourceType", equalTo("User"))
                .body("meta.created", notNullValue())
                .extract().path("id");
    }

    @Test
    public void testGetUser() {
        if (userId == null) testCreateUser();
        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .when().get("/scim/v2/Users/{id}", userId)
                .then()
                .statusCode(200)
                .body("schemas", hasItem("urn:ietf:params:scim:schemas:core:2.0:User"))
                .body("id", equalTo(userId))
                .body("userName", equalTo("scimuser1"));
    }

    @Test
    public void testUpdateUser() {
        if (userId == null) testCreateUser();
        given()
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .body("{\"userName\":\"scimuser_updated\",\"nameGiven\":\"ScimUpdated\"}")
                .when().patch("/scim/v2/Users/{id}", userId)
                .then()
                .statusCode(200)
                .body("userName", equalTo("scimuser_updated"));
    }

    @Test
    public void testDeleteUser() {
        if (userId == null) testCreateUser();
        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .when().delete("/scim/v2/Users/{id}", userId)
                .then()
                .statusCode(204);
    }

    @Test
    public void testListUsers() {
        if (tenantId == null) testCreateUser();
        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .when().get("/scim/v2/Users?count=10&startIndex=1")
                .then()
                .statusCode(200)
                .body("Resources", notNullValue())
                .body("totalResults", greaterThanOrEqualTo(0));
    }

    @Test
    public void testCreateGroup() {
        if (tenantId == null) testCreateUser();
        groupId = given()
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .body("{\"displayName\":\"SCIM Engineers\"}")
                .when().post("/scim/v2/Groups")
                .then()
                .statusCode(201)
                .body("schemas", hasItem("urn:ietf:params:scim:schemas:core:2.0:Group"))
                .body("id", notNullValue())
                .body("displayName", equalTo("SCIM Engineers"))
                .body("meta.resourceType", equalTo("Group"))
                .extract().path("id");
    }

    @Test
    public void testGetGroup() {
        if (groupId == null) testCreateGroup();
        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .when().get("/scim/v2/Groups/{id}", groupId)
                .then()
                .statusCode(200)
                .body("id", equalTo(groupId))
                .body("displayName", equalTo("SCIM Engineers"));
    }

    @Test
    public void testListGroups() {
        if (tenantId == null) testCreateUser();
        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "test-user")
                .when().get("/scim/v2/Groups?count=10&startIndex=1")
                .then()
                .statusCode(200)
                .body("Resources", notNullValue())
                .body("totalResults", greaterThanOrEqualTo(0));
    }
}
