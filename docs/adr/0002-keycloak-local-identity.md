# ADR-0002: Local Keycloak as Identity Broker

## Status

Accepted

## Context

The Identity Entitlement Broker needs an OIDC identity provider for:

1. **Development and testing**: Developers need to authenticate and obtain JWT tokens without depending on external enterprise IdPs.
2. **Demo environments**: The broker must be self-contained for demonstrations and proof-of-concept deployments.
3. **Integration testing**: Automated tests need a predictable OIDC provider that can be configured programmatically.
4. **Token generation**: Generating JWTs with the correct claims (tenant_id, roles, entitlements) for testing.

Options considered:

| Option | Description | Pros | Cons |
|---|---|---|---|
| **Keycloak Docker image** | Keycloak 26.x as Docker container | Production-quality OIDC, realm import, admin UI, well-documented | Additional container dependency, ~700MB image |
| Embedded OIDC mock | Custom mock OIDC provider within the API | No external dependency, lightweight | Not production-matching, feature gaps |
| Third-party OIDC mock | WireMock / MockServer with OIDC responses | Simple setup, flexible | No admin UI, not realistic |
| Use only real IdPs | Skip local auth, connect only to enterprise IdPs | No additional infrastructure | Cannot develop without network, slow iteration |

## Decision

We will use **Keycloak 26.x running as a Docker container** for local identity provider needs.

Rationale:
1. **Production-matching behavior**: Keycloak is a production-grade OIDC provider. Code tested against local Keycloak will behave identically against a real Keycloak instance in production.
2. **Realm import**: Keycloak supports importing realm configuration from JSON files, enabling reproducible setups.
3. **Admin UI**: The Keycloak admin console makes it easy to inspect users, tokens, and configuration during development.
4. **OCI image**: The Keycloak image is available from `quay.io/keycloak/keycloak:26.0` and works with our Docker Compose stack.
5. **Database sharing**: Keycloak can use the same MariaDB instance, reducing infrastructure complexity.
6. **Maturity**: Keycloak is the de-facto standard open-source OIDC provider, with extensive community support.

### Configuration Details

Keycloak will be configured with:
- Realm: `identity-broker`
- Client: `identity-broker-api` (confidential, service account enabled)
- Sample users with group memberships and role mappings
- Protocol mappers to include `tenant_id`, `roles`, and `entitlements` in tokens

## Consequences

### Positive

- Realistic OIDC flow during development
- Ability to test token validation, claim extraction, and role mapping end-to-end
- Can be used in CI pipelines via Testcontainers
- No dependency on external IdP availability during development
- Keycloak admin UI enables visual debugging of authentication flows

### Negative

- Additional Docker dependency (~700MB image, ~800MB allocated memory)
- Startup time for Keycloak (~10-15 seconds) adds to stack startup sequence
- Development environment does not exactly match production if production uses a different IdP (e.g., Azure AD)

### Mitigations

- Use Docker Compose `depends_on` with health checks to manage startup ordering
- Document the transition path from local Keycloak to enterprise IdP
- Keep OIDC configuration environment-driven so switching IdPs is a config change, not a code change
- Provide a `testcontainers`-based integration test harness for CI
