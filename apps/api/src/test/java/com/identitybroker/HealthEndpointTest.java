package com.identitybroker;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class HealthEndpointTest {

    @Test
    public void testHealth() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("version", equalTo("1.0.0"));
    }

    @Test
    public void testReadiness() {
        given()
                .when().get("/ready")
                .then()
                .statusCode(200)
                .body("ready", equalTo(true))
                .body("database", equalTo(true));
    }

    @Test
    public void testVersion() {
        given()
                .when().get("/version")
                .then()
                .statusCode(200)
                .body("version", notNullValue())
                .body("name", equalTo("identity-entitlement-broker"))
                .body("java", equalTo("17"));
    }
}
