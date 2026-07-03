# ADR-0003: OPA for Policy Decisions

## Status

Accepted

## Context

The Identity Entitlement Broker needs an authorization engine that supports:

1. **Externalized policy management**: Policy logic should be decoupled from application code, enabling changes without re-deployment.
2. **Auditability**: Every policy decision should be traceable to specific rules with clear reasoning.
3. **Testability**: Policies must be testable in isolation.
4. **Multi-tenant awareness**: Policies must support tenant-specific rule variations.
5. **Structured input**: Policy evaluation should accept structured JSON input (tenant, actor, action, resource, roles, entitlements).

Options considered:

| Option | Description | Pros | Cons |
|---|---|---|---|
| **OPA (Open Policy Agent)** | Rego-based policy engine, sidecar or remote | Industry standard, decoupled, testable, REST API | Additional infrastructure, Rego learning curve |
| Custom Java policy service | Embedded policy evaluation in Java | No external dependency, full Java ecosystem | Coupled to codebase, less testable, redeployment needed |
| Casbin | Go/Java policy library | Familiar RBAC/ABAC model | Limited multi-tenant support, Java version less mature |
| AWS Cedar | New policy language from AWS | Designed for fine-grained auth | Very new, limited ecosystem, AWS-centric |
| Spring Security ACL | Built-in Spring ACL | Simple setup for row-level security | Not decoupled, no external evaluation, limited expressiveness |

## Decision

We will use **OPA (Open Policy Agent) as an external policy engine**, running as a sidecar container.

Rationale:
1. **Policy-code separation**: OPA policies (Rego) live in `policies/opa/` and are loaded at startup. Policy changes only require restarting the OPA container or reloading policies via the OPA API.
2. **Auditability**: OPA provides structured decision logs with full input/output, satisfying compliance requirements.
3. **Testability**: Rego has a built-in test framework (`opa test`) with mocking support (`with input as ...`).
4. **Industry adoption**: OPA is the de-facto standard for cloud-native policy engines, used by major platforms (Kubernetes, Istio, Netflix).
5. **Backward compatibility**: The broker includes a local fallback policy evaluator for development environments where OPA is not running.
6. **Performance**: OPA caches compiled policies and evaluates in sub-millisecond time for typical rule sets.

## Consequences

### Positive

- Policy changes do not require application re-deployment
- Policies are testable independently with `opa test`
- Structured decision logs enable compliance auditing
- Clear separation of concerns between application logic and authorization
- OPA's REST API allows any client (not just the broker) to evaluate policies
- Tenant-specific policies can be loaded into OPA's data store

### Negative

- Additional infrastructure component (OPA container)
- Network latency for policy evaluation (typically <5ms with co-located OPA)
- Rego is a new language for most developers, requiring training
- OPA data must be synchronized with the broker's tenant configuration

### Mitigations

- Run OPA as a sidecar container in the same pod/network namespace
- Use OPA's bundle API to serve policies from a central repository
- Cache OPA decisions for read-heavy workloads
- Provide Rego development guidelines and test examples
- Implement local fallback for development/disconnected scenarios
- Version policies alongside the codebase in `policies/opa/`
