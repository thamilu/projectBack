# ADR-001: Use Keycloak for Identity and Access Management

## Date
2026-05-10

## Status
Accepted

## Context
The E-Shop platform requires a robust, enterprise-grade authentication and authorization system. We need to handle:
- User registration and login (OIDC/OAuth2).
- Role-Based Access Control (RBAC) for Customers, Sellers, and Admins.
- Security against common threats (Brute force, session hijacking).
- Future scalability for SSO.

## Decision
We chose **Keycloak** as the primary Identity Provider (IdP).

### Rationale:
1. **Industry Standard**: Implements OIDC, OAuth2, and SAML natively.
2. **Feature Rich**: Out-of-the-box support for user federation, identity brokering, and social login.
3. **Decoupling**: Security logic is externalized from the business backend, simplifying the Spring Boot application.
4. **Admin UI**: Provides a sophisticated dashboard for managing users and roles without custom code.

## Consequences
- **Infrastructure**: Requires a dedicated Keycloak instance (PostgreSQL + Java).
- **RAM**: Adds ~1-2GB RAM overhead to the total system.
- **Latency**: Token validation adds a minor network/CPU overhead, mitigated by JWT local validation.
- **Complexity**: Requires OIDC knowledge for the frontend (NextAuth) and backend (Spring Security).
