# ADR-0005: Tenant Isolation via Application-Level Enforcement

## Status

Accepted

## Context

The Identity Entitlement Broker is a multi-tenant system where each tenant's data must be strictly isolated from every other tenant. The core question is where to enforce this isolation: at the database layer, the application layer, or both.

Options considered:

| Option | Description | Pros | Cons |
|---|---|---|---|
| **Application-level enforcement** | @RequestScoped TenantContext filter validates every operation | Consistent enforcement, no DB changes needed, testable | Must be implemented in every service layer |
| Database row-level security | MariaDB RLS or PostgreSQL row-level security policies | DB-layer enforcement, not bypassable by application code | MariaDB RLS limited; complex migration; DB-specific |
| Separate databases per tenant | Separate MariaDB databases/schemas | Complete isolation, no cross-tenant risk | Connection pooling explosion, schema migration complexity, backup overhead |
| Tenant column + DB views | Tenant column on every table with views that filter | Read-level security built in | Views don't protect writes; maintenance burden |
| Hybrid: application + DB audit | Application-layer enforcement with DB-level audit triggers | Defense in depth | Additional complexity |

## Decision

We will use **application-level tenant isolation enforced at the JAX-RS filter layer**, with explicit tenant-ID filtering in all repository queries.

Rationale:
1. **Consistent enforcement point**: A `ContainerRequestFilter` intercepts every request, extracts the tenant context, and sets a `@RequestScoped` CDI bean. All downstream components (services, repositories) use this bean.
2. **Testability**: Application-level enforcement can be tested with standard unit/integration tests without relying on database features.
3. **Portability**: Does not depend on MariaDB-specific features; works identically across database backends.
4. **Gradual adoption**: Tenant isolation can be added to new entities without database migrations for RLS policies.
5. **Auditability**: Every tenant context violation is logged as an audit event at the application layer.

### Implementation Details

The enforcement chain:

1. **JAX-RS Filter** (`TenantContextFilter`):
   - Extracts `tenant_id` from JWT claim (production) or `X-Tenant-Id` header (dev mode)
   - Validates tenant exists and is active via lightweight cache
   - Sets `@RequestScoped TenantContext` bean
   - Returns 401/403 if tenant context is invalid

2. **Panache Repository Base**:
   - All entity repositories extend `TenantAwareRepository` 
   - Automatically adds `tenant_id = :tenantId` to queries
   - Validates tenant context on entity creation

3. **Service Layer**:
   - Validates that referenced entities belong to the tenant
   - Prevents cross-tenant resource access

## Consequences

### Positive

- Single, well-defined enforcement point (the filter and base repository)
- No database-specific features required
- Full control over error handling (404 vs 403 responses)
- Testable with mocked tenant context in unit tests
- Easy to add to new entities without database changes
- Consistent cross-tenant error responses (always 404 for resources, never 403)

### Negative

- Requires discipline in every new service/repository to use the tenant-aware base
- Bypassable if a developer forgets to extend the base repository or manually writes a native query without tenant filtering
- Slightly more code than database-level row security
- Tenant context must be explicitly propagated in asynchronous processing

### Mitigations

- **Code review checklist**: Enforce tenant isolation as a mandatory review item for every PR
- **Static analysis**: Add a custom annotation processor or ArchUnit test that verifies all repositories extend `TenantAwareRepository`
- **Integration tests**: Mandatory cross-tenant access tests in the CI pipeline
- **Audit logging**: Every tenant context failure is logged as an auditable event
- **Defense in depth**: While application-level is the primary mechanism, also consider adding database-level RLS for the most critical tables (audit_events, external_users) as a secondary layer
- **Async propagation**: TenantContext is propagated via `@RequestScoped` and explicitly passed to async processing using tenant-aware thread pools or reactive context propagation
