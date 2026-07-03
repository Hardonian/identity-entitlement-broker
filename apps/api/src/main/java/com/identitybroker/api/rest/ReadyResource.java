package com.identitybroker.api.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/ready")
@Produces(MediaType.APPLICATION_JSON)
public class ReadyResource {

    @GET
    public Map<String, Object> ready() {
        return Map.of(
                "ready", true,
                "database", true
        );
    }
}
