package com.identitybroker.api.rest.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response original = exception.getResponse();
        if (original.getEntity() != null) {
            return original;
        }
        return Response.status(original.getStatus())
                .entity(new ErrorResponse("ERROR", exception.getMessage()))
                .type("application/json")
                .build();
    }
}
