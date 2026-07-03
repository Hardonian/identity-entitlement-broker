package com.identitybroker.application;

import com.identitybroker.api.dto.IdpConnectionResponse;
import com.identitybroker.api.dto.RegisterIdpRequest;
import com.identitybroker.api.rest.exception.ConflictException;
import com.identitybroker.api.rest.exception.NotFoundException;
import com.identitybroker.domain.IdentityProviderConnection;
import com.identitybroker.domain.IdentityProviderConnection.ProviderType;
import com.identitybroker.infrastructure.audit.AuditService;
import com.identitybroker.infrastructure.persistence.IdpConnectionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class IdpConnectionService {

    @Inject
    IdpConnectionRepository idpConnectionRepository;

    @Inject
    AuditService auditService;

    @Transactional
    public IdpConnectionResponse register(UUID tenantId, @Valid RegisterIdpRequest request, String actorId) {
        // Validate provider type
        ProviderType providerType;
        try {
            providerType = ProviderType.valueOf(request.providerType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConflictException("Invalid provider type: " + request.providerType +
                    ". Must be one of: OIDC, SAML, LDAP");
        }

        IdentityProviderConnection conn = new IdentityProviderConnection();
        conn.setTenantId(tenantId);
        conn.setProviderType(providerType);
        conn.setIssuer(request.issuer);
        conn.setMetadataUrl(request.metadataUrl);
        conn.setClientId(request.clientId);
        conn.setSecretRef(request.secretRef);
        conn.setStatus(IdentityProviderConnection.IdpStatus.ACTIVE);

        idpConnectionRepository.persist(conn);

        auditService.recordSuccess(tenantId, actorId, "idp.register", "IdentityProviderConnection",
                conn.getId().toString(), "Registered IdP connection: " + providerType + " - " + request.issuer);

        return IdpConnectionResponse.from(conn);
    }

    public List<IdpConnectionResponse> listByTenant(UUID tenantId) {
        return idpConnectionRepository.findByTenantId(tenantId).stream()
                .map(IdpConnectionResponse::from)
                .collect(Collectors.toList());
    }

    public IdpConnectionResponse getConnection(UUID id) {
        IdentityProviderConnection conn = idpConnectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IdP connection not found: " + id));
        return IdpConnectionResponse.from(conn);
    }

    @Transactional
    public IdpConnectionResponse update(UUID id, @Valid RegisterIdpRequest request, String actorId) {
        IdentityProviderConnection conn = idpConnectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IdP connection not found: " + id));

        ProviderType providerType;
        try {
            providerType = ProviderType.valueOf(request.providerType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConflictException("Invalid provider type: " + request.providerType);
        }

        conn.setProviderType(providerType);
        conn.setIssuer(request.issuer);
        conn.setMetadataUrl(request.metadataUrl);
        conn.setClientId(request.clientId);
        if (request.secretRef != null) {
            conn.setSecretRef(request.secretRef);
        }
        idpConnectionRepository.persist(conn);

        auditService.recordSuccess(conn.getTenantId(), actorId, "idp.update", "IdentityProviderConnection",
                conn.getId().toString(), "Updated IdP connection");

        return IdpConnectionResponse.from(conn);
    }

    @Transactional
    public void remove(UUID id, String actorId) {
        IdentityProviderConnection conn = idpConnectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IdP connection not found: " + id));

        idpConnectionRepository.delete(conn);

        auditService.recordSuccess(conn.getTenantId(), actorId, "idp.remove", "IdentityProviderConnection",
                conn.getId().toString(), "Removed IdP connection");
    }

    public boolean validateConnection(UUID id) {
        IdentityProviderConnection conn = idpConnectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IdP connection not found: " + id));
        // For demo: check that the connection exists and is active
        return conn.getStatus() == IdentityProviderConnection.IdpStatus.ACTIVE;
    }
}
