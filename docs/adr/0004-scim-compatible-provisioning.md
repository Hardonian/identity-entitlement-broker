# ADR-0004: SCIM-Compatible Provisioning API

## Status

Accepted

## Context

The Identity Entitlement Broker needs a standardized API for user and group lifecycle management. Enterprise identity providers (Azure AD, Okta, OneLogin) and HRIS systems (Workday, BambooHR) support the SCIM 2.0 protocol (RFC 7642-7644) for automated provisioning.

Options considered:

| Option | Description | Pros | Cons |
|---|---|---|---|
| **SCIM-compatible endpoints** | Implement SCIM-like endpoints without full RFC compliance | Enterprise IdP compatibility, standard pattern, documented deviations | Some systems may expect full compliance |
| Full SCIM 2.0 spec | Strict RFC 7644 implementation with all extensions | Maximum compatibility, schema validation | Significant implementation effort, many features unused |
| Custom provisioning API | Proprietary REST API | Simple, tailored to domain | No IdP compatibility, every integration requires custom adapter |
| System API + SCIM gateway | Implement system API with SCIM translation layer | Separation of concerns | Added complexity, dual API maintenance |

## Decision

We will implement **SCIM-compatible endpoints** that follow the SCIM 2.0 protocol shape but explicitly document deviations from the full specification.

Rationale:
1. **Enterprise compatibility**: Azure AD and Okta expect SCIM endpoints for provisioning. SCIM compliance is a prerequisite for many enterprise customers.
2. **Reduced complexity**: Full SCIM spec compliance would require significant implementation effort for features (bulk operations, schema discovery, complex filtering) that we don't need.
3. **Documented deviations**: By clearly documenting where we deviate from SCIM, customers and IdP operators can configure their systems accordingly.
4. **Familiar pattern**: SCIM's RESTful pattern maps naturally to the broker's domain model, even with deviations.
5. **Future-proofing**: The SCIM-shaped API can evolve toward full compliance as requirements grow.

### Documented Deviations

1. **No `/scim/v2/Schemas` endpoint**: Schema discovery is not implemented. Schemas are documented in the OpenAPI spec.
2. **No `/scim/v2/Bulk` endpoint**: Bulk operations are not supported. Each provisioning operation is individual.
3. **Limited filter support**: Only `eq`, `co`, `sw` operators on `userName`, `displayName`, `active`, and `externalId`.
4. **No enterprise extension**: The `urn:ietf:params:scim:schemas:extension:enterprise:2.0:User` extension is not implemented.
5. **No password management**: The broker does not set or manage passwords; authentication is handled by the external IdP.
6. **Soft delete only**: DELETE operations deactivate rather than hard-delete, with configurable retention before permanent removal.
7. **Content type**: We accept both `application/scim+json` and `application/json` for compatibility.

## Consequences

### Positive

- Enterprise IdPs can connect and provision users/groups without custom adapters
- Standard REST endpoints with SCIM-like payloads are easy to test and debug
- Documented deviations set clear expectations for integrators
- Reduced implementation effort compared to full SCIM compliance
- Compatible with Azure AD SCIM provisioning and Okta SCIM integration

### Negative

- Some IdPs may require full SCIM compliance and refuse to connect without schema discovery
- Users familiar with SCIM may expect features (bulk operations, complex filters) that are not implemented
- Need to maintain documentation of deviations as the implementation evolves

### Mitigations

- Clearly document deviations in the integration documentation and onboarding runbook
- Accept both `application/scim+json` and `application/json` to ensure broad compatibility
- Provide integration test cases for common IdP provisioning scenarios
- Implement a compatibility checklist for IdP operators to verify before production
- Revisit full compliance if customer demand justifies the investment
