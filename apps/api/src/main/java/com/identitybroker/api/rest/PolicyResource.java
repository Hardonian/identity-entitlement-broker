package com.identitybroker.api.rest;

import com.identitybroker.api.dto.CheckAccessRequest;
import com.identitybroker.api.dto.PolicyDecisionRequest;
import com.identitybroker.api.dto.PolicyDecisionResponse;
import com.identitybroker.application.PolicyService;
import com.identitybroker.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

@Path("/api/v1/policy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Policy", description = "Policy evaluation and access control")
public class PolicyResource {

    @Inject
    PolicyService policyService;

    @Inject
    TenantContext tenantContext;

    @POST
    @Path("/decide")
    @Operation(summary = "Evaluate a policy decision")
    @APIResponse(responseCode = "200", description = "Policy decision result")
    public PolicyDecisionResponse decide(@Valid PolicyDecisionRequest request) {
        return policyService.decide(request, tenantContext.getCurrentActorId());
    }

    @POST
    @Path("/check-access")
    @Operation(summary = "Quick access check for a user and product")
    @APIResponse(responseCode = "200", description = "Access check result")
    public Map<String, Object> checkAccess(CheckAccessRequest request) {
        PolicyDecisionRequest pdr = new PolicyDecisionRequest();
        pdr.tenantId = request.tenantId;
        pdr.action = "access:" + request.productSlug;
        pdr.subject = new PolicyDecisionRequest.SubjectInfo();
        pdr.subject.id = request.userId;
        pdr.subject.type = "user";
        pdr.subject.roles = java.util.List.of();

        PolicyDecisionResponse decision = policyService.decide(pdr, tenantContext.getCurrentActorId());

        return Map.of(
                "allowed", decision.allowed,
                "reason", decision.reason,
                "tenantId", request.tenantId,
                "userId", request.userId,
                "productSlug", request.productSlug
        );
    }
}
