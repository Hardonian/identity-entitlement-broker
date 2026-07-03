package com.identitybroker.api.rest.exception;

import com.identitybroker.api.dto.ErrorResponse;
import com.identitybroker.infrastructure.security.CrossTenantAccessException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Maps {@link CrossTenantAccessException} to HTTP 403 with a structured error response.
 */
@Provider
public class CrossTenantAccessExceptionMapper
        implements ExceptionMapper<CrossTenantAccessException> {

    private static final Logger log = LoggerFactory.getLogger(CrossTenantAccessExceptionMapper.class);

    @Override
    public Response toResponse(CrossTenantAccessException exception) {
        log.warn("Cross-tenant access denied: {}", exception.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .status(Response.Status.FORBIDDEN.getStatusCode())
            .code("CROSS_TENANT_ACCESS_DENIED")
            .message(exception.getMessage())
            .details(List.of(
                "Expected tenant: " + exception.getTenantId(),
                "Resource: " + exception.getResourceId()
            ))
            .timestamp(LocalDateTime.now())
            .build();

        return Response.status(Response.Status.FORBIDDEN)
            .entity(error)
            .build();
    }
}
