package com.identitybroker.api.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "System", description = "System health and monitoring endpoints")
public class HealthResource {

    @GET
    @Operation(summary = "Health check", description = "Returns the current health status of the API")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
