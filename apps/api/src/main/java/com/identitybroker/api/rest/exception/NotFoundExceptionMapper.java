package com.identitybroker.api.rest.exception;

import com.identitybroker.api.dto.ErrorResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Maps {@link NotFoundException} (JAX-RS resource not found) to HTTP 404.
 */
@Provider
public class NotFoundExceptionMapper
        implements ExceptionMapper<NotFoundException> {

    private static final Logger log = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    public Response toResponse(NotFoundException exception) {
        log.debug("Resource not found: {}", exception.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .status(Response.Status.NOT_FOUND.getStatusCode())
            .code("RESOURCE_NOT_FOUND")
            .message(exception.getMessage() != null ? exception.getMessage() : "The requested resource was not found")
            .timestamp(LocalDateTime.now())
            .build();

        return Response.status(Response.Status.NOT_FOUND)
            .entity(error)
            .build();
    }
}
