package com.identitybroker.api.dto;

import com.identitybroker.domain.IdentityProviderConnection;
import java.time.LocalDateTime;
import java.util.UUID;

public class IdpConnectionResponse {

    public UUID id;
    public UUID tenantId;
    public String providerType;
    public String issuer;
    public String metadataUrl;
    public String clientId;
    public String secretRef;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public static IdpConnectionResponse from(IdentityProviderConnection conn) {
        IdpConnectionResponse r = new IdpConnectionResponse();
        r.id = conn.getId();
        r.tenantId = conn.getTenantId();
        r.providerType = conn.getProviderType().name();
        r.issuer = conn.getIssuer();
        r.metadataUrl = conn.getMetadataUrl();
        r.clientId = conn.getClientId();
        r.secretRef = maskSecret(conn.getSecretRef());
        r.status = conn.getStatus().name();
        r.createdAt = conn.getCreatedAt();
        r.updatedAt = conn.getUpdatedAt();
        return r;
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 4) return "****";
        return "****" + secret.substring(secret.length() - 4);
    }
}
