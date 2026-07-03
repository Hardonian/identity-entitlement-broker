# ADR-0001: Quarkus + Java as Backend Framework

## Status

Accepted

## Context

The Identity Entitlement Broker requires a backend framework that supports:

- **Reactive and imperative programming models**: The broker handles both high-throughput SCIM provisioning (reactive) and traditional CRUD operations (imperative).
- **Cloud-native deployment**: Container-friendly, small memory footprint, fast startup times.
- **Built-in OIDC support**: Native integration with OpenID Connect for JWT validation and token processing.
- **Object-relational mapping**: Panache ORM for simplified database access with tenant isolation.
- **REST API development**: JAX-RS standard with OpenAPI generation.
- **Native compilation**: Option for GraalVM native images for even faster startup and lower memory.

Options considered:

| Framework | Language | Startup | Memory | OIDC Support | ORM |
|---|---|---|---|---|---|
| **Quarkus 3.x** | Java 17 | ~1s | ~50MB | Built-in (OIDC, JWT) | Panache (Hibernate) |
| Spring Boot 3.x | Java 17 | ~3s | ~150MB | Spring Security OAuth2 | Spring Data JPA |
| FastAPI | Python | ~0.3s | ~50MB | Authlib / python-jose | SQLAlchemy |
| Gin + Casbin | Go | ~0.1s | ~10MB | Manual JWT validation | GORM |
| Express + Passport | Node.js | ~0.3s | ~30MB | Passport.js | TypeORM |

## Decision

We will use **Quarkus 3.x with Java 17**.

Rationale:
1. **Reactive + imperative**: Quarkus supports both Mutiny (reactive) and imperative programming in the same application.
2. **OIDC integration**: Quarkus OIDC extension (`quarkus-oidc`) provides built-in token validation, tenant resolution, and role extraction with minimal configuration.
3. **Panache ORM**: Active Record pattern via Panache simplifies repository code and enables tenant-scoped queries.
4. **Fast startup and low memory**: Quarkus's build-time processing and Substrate VM compatibility make it ideal for containerized deployments.
5. **Developer productivity**: Dev mode with hot reload (`mvn quarkus:dev`) significantly improves development iteration speed.
6. **OpenAPI generation**: Built-in OpenAPI and Swagger UI generation from JAX-RS annotations.

While Go and Python options offer faster startup and lower resource usage, Quarkus's mature OIDC and ORM ecosystem outweights these advantages for this project's requirements.

## Consequences

### Positive

- Strong typing and compile-time safety for complex domain model
- Mature ORM (Hibernate/Panache) with proven multi-tenant patterns
- Robust ecosystem for OIDC, REST, and database interactions
- Hot-reload development mode
- Build-time metadata processing produces optimized native images
- Large community and extensive documentation

### Negative

- Heavier than Go or Python equivalents in terms of final binary size
- Java ecosystem has steeper learning curve for developers coming from dynamic languages
- Build times are longer than interpreted languages (even with incremental compilation)

### Mitigations

- Use Quarkus's build-time processing to minimize runtime overhead
- Structure the project with clear module boundaries using Quarkus extensions
- Leverage GraalVM native compilation for production deployments where startup time is critical
- Document the build process and IDE setup thoroughly for developer onboarding
