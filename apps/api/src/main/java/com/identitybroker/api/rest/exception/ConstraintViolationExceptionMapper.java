package com.identitybroker.api.rest.exception;

import com.identitybroker.api.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@link ConstraintViolationException} (bean validation failures)
 * to HTTP 400 with field-level detail messages.
 */
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger log = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
            .map(this::formatViolation)
            .collect(Collectors.toList());

        log.debug("Validation constraint violation: {}", details);

        ErrorResponse error = ErrorResponse.builder()
            .status(Response.Status.BAD_REQUEST.getStatusCode())
            .code("VALIDATION_ERROR")
            .message("Request validation failed")
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(error)
            .build();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() != null
            ? violation.getPropertyPath().toString()
            : "unknown";
        return field + ": " + violation.getMessage();
    }
}
