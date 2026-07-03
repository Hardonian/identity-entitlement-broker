package com.identitybroker.api.dto;

import com.identitybroker.domain.IdentityProviderConnection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RegisterIdpRequest")
public class RegisterIdpRequest {

    @NotNull(message = "Provider type is required")
    @Schema(description = "Identity provider type", required = true, example = "OIDC")
    public String providerType;

    @NotBlank(message = "Issuer URL is required")
    @Schema(description = "Issuer URL for the identity provider", required = true, example = "https://auth.acme.com/oidc")
    public String issuer;

    @Schema(description = "Metadata URL", example = "https://auth.acme.com/oidc/.well-known/openid-configuration")
    public String metadataUrl;

    @Schema(description = "Client ID", example = "client-abc-123")
    public String clientId;

    @Schema(description = "Secret reference (will be masked in responses)", example = "vault://idp/secret-ref")
    public String secretRef;
}
