package com.identitybroker.api.rest.exception;

import com.identitybroker.api.dto.ErrorResponse;
import com.identitybroker.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Catch-all exception mapper for unhandled exceptions.
 * Returns HTTP 500 with a correlation ID for traceability.
 */
@Provider
public class GeneralExceptionMapper
        implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(GeneralExceptionMapper.class);

    @Inject
    TenantContext tenantContext;

    @Override
    public Response toResponse(Throwable exception) {
        String correlationId = tenantContext != null ? tenantContext.getCorrelationId() : null;
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        log.error("Unhandled exception (correlationId={}): {}", correlationId, exception.getMessage(), exception);

        ErrorResponse error = ErrorResponse.builder()
            .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred. Please reference correlation ID for support.")
            .details(List.of("correlationId: " + correlationId))
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(error)
            .build();
    }
}
