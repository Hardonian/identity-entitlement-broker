package com.identitybroker.api.rest.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class CrossTenantException extends WebApplicationException {
    public CrossTenantException(String message) {
        super(Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("CROSS_TENANT_ACCESS", message))
                .type("application/json")
                .build());
    }
}
