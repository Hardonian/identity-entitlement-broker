# Entitlement Resolution

## Overview

Entitlements are the atomic units of authorization in the Identity Entitlement Broker. They represent permission to perform actions on a product. The entitlement model uses a three-layer hierarchy: **Products → Entitlements → Assignments**. Users gain access through assignments (direct or group-based), and the effective set is always the union of all active assignments.

## Entitlement Model

```mermaid
graph TD
  subgraph Products["Products"]
    P1[Identity Core]
    P2[Analytics Platform]
    P3[Audit Service]
  end

  subgraph Entitlements["Entitlements"]
    E1[identity-admin] --> P1
    E2[user-manager] --> P1
    E3[support-tools] --> P1
    E4[analytics-admin] --> P2
    E5[analytics-dash] --> P2
    E6[audit-viewer] --> P3
  end

  subgraph Assignments["Assignments"]
    A1[Direct: User A -> identity-admin]
    A2[Group: Identity Admins -> identity-admin + user-manager]
    A3[Group: All Employees -> basic-view]
    A4[Direct: User B -> audit-viewer]
  end

  subgraph Effective["Effective Entitlements"]
    UserA_Eff["User A: identity-admin, user-manager, basic-view"]
    UserB_Eff["User B: identity-admin, user-manager, audit-viewer, basic-view"]
  end

  Assignments --> Effective
```

## Hierarchy

### Product

A product represents a logical service or capability group. Examples:
- `identity-core`: Identity management services
- `analytics-platform`: Analytics and reporting
- `audit-service`: Audit log access
- `billing-service`: Billing management

### Entitlement

An entitlement is a named permission scoped to a product. Examples:
- `identity-admin` (product: `identity-core`): Full administrative access to identity services
- `user-manager` (product: `identity-core`): User provisioning and lifecycle management
- `audit-viewer` (product: `audit-service`): Read-only access to audit logs

### Assignment

An assignment links a user (or group) to an entitlement. Assignment types:

| Type | Description | Source |
|---|---|---|
| `DIRECT` | User individually assigned | Manual admin action or API |
| `GROUP` | User inherits via group membership | SCIM provisioning or IdP sync |

## Effective Entitlement Resolution

The broker resolves effective entitlements by computing the union of all active assignments for a user:

```mermaid
flowchart TD
  Start([Resolve for User]) --> Direct[Get DIRECT assignments]
  Direct --> GroupMemberships[Get user's group memberships]
  GroupMemberships --> GroupAssign[Get GROUP assignments<br/>for all groups]
  GroupAssign --> Union[Union all entitlement IDs]
  Union --> FilterActive[Filter to active entitlements]
  FilterActive --> ResolveProduct[Resolve product slugs]
  ResolveProduct --> Return[Return effective entitlements<br/>with source metadata]

  Start --> PolicyCheck[Also feed to OPA<br/>for policy decisions]
```

### Resolution Algorithm

```
Input: userId (UUID)
Output: List of Effective Entitlements (entitlementId, name, slug, productSlug, assignmentType)

1. DIRECT: SELECT assignments WHERE user_id = :userId AND type = 'DIRECT' AND active = true
2. GROUP:   SELECT user's group memberships
            SELECT assignments WHERE group_id IN (user's groups) AND active = true
3. UNION:   entitlement_ids = DIRECT.entitlement_ids ∪ GROUP.entitlement_ids
4. RESOLVE: SELECT entitlements WHERE id IN entitlement_ids
5. RETURN:  Map to EffectiveEntitlement DTO with type metadata
```

## Resolution Sequence

```mermaid
sequenceDiagram
  participant Client as API Client
  participant Resource as EntitlementResource
  participant Service as EntitlementService
  participant DB as MariaDB
  participant Cache as Entitlement Cache

  Client->>Resource: GET /api/v1/assignments/user/{userId}
  Resource->>Service: getEffectiveEntitlements(userId)
  
  Service->>Cache: Check cache for user entitlements
  alt Cache hit
    Cache->>Service: Cached entitlements
  else Cache miss
    Service->>DB: Query DIRECT assignments
    DB->>Service: [assignment records]
    Service->>DB: Query user's group memberships
    DB->>Service: [group IDs]
    Service->>DB: Query GROUP assignments for those groups
    DB->>Service: [group assignment records]
    Service->>Service: Union entitlement IDs
    Service->>DB: Query entitlement details (names, slugs, products)
    DB->>Service: [entitlement details]
    Service->>Cache: Store in cache (TTL: 5 min)
  end

  Service->>Resource: List<EffectiveEntitlement>
  Resource->>Client: 200 OK (JSON array)
```

## Entitlement Evaluation in OPA

When a policy decision is requested, effective entitlements are included in the OPA input:

```json
{
  "tenant_id": "a1b2c3d4-...",
  "actor": "jane.doe@example.com",
  "subject": "jane.doe@example.com",
  "action": "access",
  "resource": "identity-core",
  "roles": ["admin"],
  "entitlements": ["identity-admin"]
}
```

The OPA policy checks whether any of the user's roles grant an entitlement that covers the requested resource product:

```rego
role_has_entitlement {
  some role
  input.roles[_] == role
  role_mapping := tenant_config.role_mappings[role]
  some entitlement
  role_mapping.entitlements[_] == entitlement
  entitlement_config := tenant_config.entitlements[entitlement]
  entitlement_config.product == input.resource
}
```

## Best Practices

1. **Prefer group-based assignments**: Group assignments scale better than individual assignments for large organizations.
2. **Limit entitlement granularity**: Create entitlements at a meaningful granularity (not too coarse, not too fine).
3. **Use descriptive slugs**: `identity-admin` is better than `ent-001` for readability and policy authoring.
4. **Document entitlement meanings**: Maintain a mapping of entitlement slug → description for auditors.
5. **Audit all changes**: Every assignment and revocation generates an immutable audit event.
