package com.identitybroker.infrastructure.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

/**
 * Simple event publisher for domain events.
 * In production, this would publish to Kafka, AMQP, etc.
 */
@ApplicationScoped
public class EventPublisher {

    private static final Logger LOG = Logger.getLogger(EventPublisher.class.getName());

    public void publish(String eventType, String payload) {
        LOG.info("Event published: " + eventType + " | " + payload);
        // In production, publish to message broker
    }

    public void publishProvisioningEvent(String tenantId, String userId, String action) {
        publish("provisioning." + action,
                String.format("{\"tenantId\":\"%s\",\"userId\":\"%s\",\"action\":\"%s\"}", tenantId, userId, action));
    }

    public void publishAuditEvent(String tenantId, String actorId, String action, String resourceType, String resourceId, String outcome) {
        publish("audit." + outcome,
                String.format("{\"tenantId\":\"%s\",\"actorId\":\"%s\",\"action\":\"%s\",\"resourceType\":\"%s\",\"resourceId\":\"%s\"}",
                        tenantId, actorId, action, resourceType, resourceId));
    }
}
