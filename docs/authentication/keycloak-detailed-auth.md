# Keycloak Detailed Authentication Guide

## Overview
This document explains Keycloak authentication concepts and provides practical examples for integrating Keycloak with applications (including Spring Boot). It covers realms, clients, roles, authentication flows, tokens, token handling, admin commands, deployment, troubleshooting, and security best practices.

## Key Concepts
- Realm: Tenant that isolates users, clients, roles, and configuration.
- Client: An application or service that requests authentication/authorization from Keycloak. Clients can be public (no secret) or confidential (has secret).
- Protocols: Typically OpenID Connect (OIDC) for web/mobile apps and OAuth2 for service-to-service.
- Roles: Define permissions; can be realm-level or client-level.
- Mappers: Map Keycloak attributes/claims into tokens (e.g., roles -> `realm_access.roles`).

## Authentication Flows

- Authorization Code (recommended for web apps): Server-side apps redirect users to Keycloak login, receive an authorization code, and exchange it for tokens.
- Authorization Code + PKCE (recommended for native & SPA): Adds PKCE for enhanced security for public clients.
- Client Credentials (machine-to-machine): Client authenticates using its credentials to obtain an access token. No user involved.
- Resource Owner Password Credentials (ROPC): Deprecated/ discouraged — app exchanges username/password for tokens directly.
- Direct Grant: Similar to ROPC; use only when necessary.

## Tokens
- ID Token: Identifies the user (OIDC). Typically consumed by the client.
- Access Token: Sent to resource servers to authorize requests. Usually short-lived.
- Refresh Token: Used to obtain new access tokens. Handle securely; rotate as needed.

Token format: JWT (signed, optionally encrypted). Validate signature, issuer (`iss`), audience (`aud`), expiry (`exp`).

## Token Introspection & Revocation
- Introspection endpoint: Allows resource servers to validate tokens server-side (useful for opaque tokens).
- Revocation: Use token revocation endpoints or admin commands to revoke refresh tokens or user sessions.

## Roles, Groups, and Mappers
- Realm roles: Global roles available across clients.
- Client roles: Scoped to a specific client.
- Groups: Assign roles to groups; assign users to groups for easier management.
- Mappers: Configure which claims appear in tokens (e.g., map LDAP attributes to `email`).

## Multi-Factor Authentication (MFA)
- Keycloak supports OTP (TOTP), WebAuthn, and other authenticators.
- Configure required actions (e.g., `Configure OTP`) on the realm authentication flows.

## Single Sign-On (SSO) and Sessions
- SSO works within a realm across clients.
- Session management: Admin console can list and revoke sessions; clients can trigger back-channel or front-channel logout.

## Logout
- Front-channel logout: Browser redirects to Keycloak logout endpoint.
- Back-channel logout: Server-to-server notification for logout.

## Securing a Spring Boot Application (Resource Server)

1) Add dependency (Gradle example):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.springframework.boot:spring-boot-starter-security'
```

2) application.properties example for Resource Server (validate JWT):

```properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://<KEYCLOAK_HOST>/auth/realms/<REALM>/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://<KEYCLOAK_HOST>/auth/realms/<REALM>
```

3) Minimal WebSecurityConfigurer (if needed):

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
            .and().oauth2ResourceServer().jwt();
    }
}
```

4) Mapping Keycloak roles to Spring authorities: use JWT `realm_access.roles` or `resource_access.<client>.roles` with a custom converter.

## Securing a Spring Boot Application (OAuth2 Client)
- For apps acting as OAuth clients (web apps using Authorization Code): use `spring-security-oauth2-client` and configure `spring.security.oauth2.client` properties with client id, secret and issuer.

application.properties example (client):

```properties
spring.security.oauth2.client.registration.keycloak.client-id=frontend-app
spring.security.oauth2.client.registration.keycloak.client-secret=<secret>
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.provider.keycloak.issuer-uri=https://<KEYCLOAK_HOST>/auth/realms/<REALM>
```

## Common CURL Examples

- Get token (client_credentials):

```bash
curl -X POST \
  -d 'grant_type=client_credentials' \
  -d 'client_id=<client>' \
  -d 'client_secret=<secret>' \
  https://<KEYCLOAK_HOST>/auth/realms/<REALM>/protocol/openid-connect/token
```

- Exchange auth code for tokens (server-side flow):

```bash
curl -X POST \
  -d 'grant_type=authorization_code' \
  -d 'code=<code>' \
  -d 'redirect_uri=<redirect>' \
  -d 'client_id=<client>' \
  -d 'client_secret=<secret>' \
  https://<KEYCLOAK_HOST>/auth/realms/<REALM>/protocol/openid-connect/token
```

- Introspect token:

```bash
curl -X POST \
  -d 'token=<access_token>' \
  -u '<client>:<secret>' \
  https://<KEYCLOAK_HOST>/auth/realms/<REALM>/protocol/openid-connect/token/introspect
```

## Keycloak Admin CLI & REST Examples

- Create a realm (simplified CLI):

```bash
kcadm.sh create realms -s realm=myrealm -s enabled=true
```

- Create client, role, and user via REST or `kcadm` scripts. Use admin credentials or service account token for automation.

## Running Keycloak with Docker (minimal)

```yaml
version: '3'
services:
  keycloak:
    image: quay.io/keycloak/keycloak:latest
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    command: start-dev
    ports:
      - '8080:8080'
```

Access admin console: http://localhost:8080/ (create realm, clients, mappers)

## Troubleshooting
- Invalid token: check `aud`, `iss`, `exp`, and JWKs endpoint.
- 401 from resource server: ensure `jwk-set-uri` and `issuer-uri` are correct and reachable.
- Missing roles: verify mappers and that roles are included in tokens (`realm_access` / `resource_access`).
- CORS issues for SPAs: configure `Web Origins` in client settings.

## Security Best Practices
- Use Authorization Code + PKCE for SPAs and native apps.
- Use short-lived access tokens and rotate refresh tokens.
- Prefer confidential clients for server-side apps.
- Restrict redirect URIs and set strict origins.
- Enable HTTPS in production and secure admin credentials.
- Use fine-grained roles and least privilege.

## References & Further Reading
- Keycloak Docs: https://www.keycloak.org/documentation
- OIDC Spec: https://openid.net/specs/
- OAuth2 RFC: https://datatracker.ietf.org/doc/html/rfc6749

---
End of guide. Feel free to request examples tailored to your application (Spring Boot, SPA, or service-to-service).
