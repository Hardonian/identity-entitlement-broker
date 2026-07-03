package com.identitybroker.infrastructure.policies;

import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging filter for OPA REST client calls.
 * Logs request and response details for debugging OPA integration issues.
 */
@Provider
public class OpaClientLoggingFilter {

    private static final Logger log = LoggerFactory.getLogger(OpaClientLoggingFilter.class);

    // Empty marker class — REST client providers are registered via @Provider
}
