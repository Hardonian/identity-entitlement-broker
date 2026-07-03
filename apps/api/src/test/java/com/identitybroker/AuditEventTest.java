package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AuditEventTest {

    private static String tenantId;
    private static String auditEventId;

    @Test
    public void testActionCreatesAuditEvent() {
        // Perform a CRUD action that generates audit
        tenantId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "audit-test-user")
                .body("{\"name\":\"Audit Tenant\",\"slug\":\"audit-tenant\"}")
                .when().post("/api/v1/tenants")
                .then().statusCode(201).extract().path("id");

        // Query audit events - should have at least the tenant creation event
        // Since audit API requires X-Tenant-Id and the tenant.create event has tenantId=null,
        // we need a different approach. Let's create an entitlement to generate an audit event
        // and then search for it.

        // Create a product (generates audit)
        given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "audit-test-user")
                .body("{\"name\":\"Audit Product\",\"slug\":\"audit-product\"}")
                .when().post("/api/v1/products")
                .then().statusCode(201);

        // Query audit endpoint with this tenant's ID
        // We need an event that has this tenantId - let's try to create an entitlement that
        // uses this tenant
        String productId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "audit-test-user")
                .body("{\"name\":\"Audit Product 2\",\"slug\":\"audit-product-2\"}")
                .when().post("/api/v1/products")
                .then().statusCode(201).extract().path("id");

        String entitlementId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "audit-test-user")
                .body("{\"productId\":\"" + productId + "\",\"name\":\"Audit Access\"," +
                        "\"slug\":\"audit-access\"}")
                .when().post("/api/v1/entitlements")
                .then().statusCode(201).extract().path("id");

        // Now create an assignment with our tenant
        String assignmentId = given()
                .header("Content-Type", "application/json")
                .header("X-Actor-Id", "audit-test-user")
                .body("{\"tenantId\":\"" + tenantId + "\",\"entitlementId\":\"" + entitlementId + "\"}")
                .when().post("/api/v1/assignments")
                .then().statusCode(201).extract().path("id");

        // Now query audit with X-Tenant-Id
        var events = given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "audit-test-user")
                .when().get("/api/v1/audit")
                .then()
                .statusCode(200)
                .body("$", notNullValue())
                .body("size()", greaterThan(0))
                .extract().path("");

        // Extract first event ID
        auditEventId = given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "audit-test-user")
                .when().get("/api/v1/audit")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .extract().path("[0].id");
    }

    @Test
    public void testAuditHasRequiredFields() {
        if (auditEventId == null) testActionCreatesAuditEvent();

        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "audit-test-user")
                .when().get("/api/v1/audit/{id}", auditEventId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("tenantId", notNullValue())
                .body("actorId", notNullValue())
                .body("action", notNullValue())
                .body("resourceType", notNullValue())
                .body("resourceId", notNullValue())
                .body("outcome", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    public void testSearchAudit() {
        if (tenantId == null) testActionCreatesAuditEvent();

        given()
                .header("X-Tenant-Id", tenantId)
                .header("X-Actor-Id", "audit-test-user")
                .queryParam("action", "entitlement.assign")
                .queryParam("resourceType", "EntitlementAssignment")
                .when().get("/api/v1/audit/search")
                .then()
                .statusCode(200)
                .body("results", notNullValue())
                .body("total", greaterThanOrEqualTo(0));
    }
}
