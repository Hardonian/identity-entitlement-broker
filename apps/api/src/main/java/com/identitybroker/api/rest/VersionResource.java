package com.identitybroker.api.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {

    @GET
    public Map<String, Object> version() {
        return Map.of(
                "version", "1.0.0",
                "name", "identity-entitlement-broker",
                "java", "17"
        );
    }
}
