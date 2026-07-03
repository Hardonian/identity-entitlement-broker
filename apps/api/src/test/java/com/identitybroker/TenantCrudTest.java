package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TenantCrudTest {

    private static String createdTenantId;

    @Test
    public void testCreateTenant() {
        createdTenantId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Test Tenant\",\"slug\":\"test-tenant\"}")
                .when().post("/api/v1/tenants")
                .then()
                .statusCode(201)
                .body("name", equalTo("Test Tenant"))
                .body("slug", equalTo("test-tenant"))
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    public void testGetTenant() {
        if (createdTenantId == null) {
            testCreateTenant();
        }
        given()
                .header("X-Actor-Id", "test-user")
                .when().get("/api/v1/tenants/{id}", createdTenantId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdTenantId))
                .body("name", notNullValue());
    }

    @Test
    public void testListTenants() {
        given()
                .header("X-Actor-Id", "test-user")
                .when().get("/api/v1/tenants")
                .then()
                .statusCode(200)
                .body("$", notNullValue())
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    public void testUpdateTenant() {
        if (createdTenantId == null) {
            testCreateTenant();
        }
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "test-user")
                .body("{\"name\":\"Updated Tenant\",\"slug\":\"updated-tenant\"}")
                .when().put("/api/v1/tenants/{id}", createdTenantId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Tenant"))
                .body("slug", equalTo("updated-tenant"));
    }

    @Test
    public void testDeleteTenant() {
        if (createdTenantId == null) {
            testCreateTenant();
        }
        given()
                .header("X-Actor-Id", "test-user")
                .when().delete("/api/v1/tenants/{id}", createdTenantId)
                .then()
                .statusCode(204);
    }
}
