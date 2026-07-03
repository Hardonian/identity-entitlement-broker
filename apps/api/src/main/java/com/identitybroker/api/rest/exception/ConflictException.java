package com.identitybroker.api.rest.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ConflictException extends WebApplicationException {
    public ConflictException(String message) {
        super(Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("CONFLICT", message))
                .type("application/json")
                .build());
    }
}
