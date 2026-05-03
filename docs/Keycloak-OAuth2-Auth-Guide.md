# Keycloak OAuth2 Authentication & Best Practices Documentation

## 1. What is Keycloak?
- Keycloak is an open-source Identity and Access Management (IAM) solution.
- It provides Single Sign-On (SSO), OAuth2, OpenID Connect (OIDC), and centralized user/role management.

## 2. What is OAuth2/OIDC Authentication?
- OAuth2 is a standard protocol for authorization (granting access to APIs).
- OpenID Connect (OIDC) is an authentication layer on top of OAuth2.
- With Keycloak, users authenticate via a secure login page, and applications receive a signed JWT (access token) to access APIs.

## 3. What is a Bearer Token?
- A bearer token is an access token (usually a JWT) sent in the HTTP Authorization header.
- Whoever possesses the token (the "bearer") can access protected resources.
- Example: `Authorization: Bearer <token>`

## 4. Keycloak OAuth2 Login Flow (Recommended)
1. User tries to access a protected resource.
2. The app redirects the user to Keycloak's login page.
3. User logs in; Keycloak authenticates and redirects back with an authorization code.
4. The app exchanges the code for an access token (JWT).
5. The app uses the token to access APIs; APIs validate the token signature and claims.

## 5. Spring Boot Configuration for Keycloak
- In `application.properties`:
  ```
  spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/eshop
  ```
- No need to set the public key manually; Spring Boot fetches it from Keycloak.

## 6. Testing with Postman
- Obtain a token from Keycloak's token endpoint using the password or authorization code grant.
- Use the token in the `Authorization: Bearer <token>` header for API requests.

## 7. Roles & Business Logic
- Keycloak manages users and roles (e.g., seller, admin).
- Your backend enforces business rules (e.g., only sellers can create their own products; admins can manage all products).
- Product ownership is tracked in your database, not in Keycloak.

## 8. Why Not Use Custom JWT Auth?
- Custom JWT (HS256) is less secure and harder to manage than OAuth2/OIDC with Keycloak.
- Keycloak provides SSO, user management, password reset, social login, and more out of the box.
- Use Keycloak for all authentication and authorization for better security and scalability.

## 9. Migration Steps (for Later Implementation)
1. Remove custom login endpoints and JWT generation from your backend.
2. Configure Spring Boot as an OAuth2 Resource Server.
3. Redirect users to Keycloak for login (OIDC flow).
4. Use Keycloak-issued RS256 tokens for all API authentication.
5. Enforce business logic and permissions in your backend using token claims (roles, user ID).

## 10. Useful Endpoints
- Keycloak login: `http://localhost:8080/realms/eshop/account`
- Token endpoint: `http://localhost:8080/realms/eshop/protocol/openid-connect/token`
- OpenID config: `http://localhost:8080/realms/eshop/.well-known/openid-configuration`
- JWKS (public keys): `http://localhost:8080/realms/eshop/protocol/openid-connect/certs`

---

**Summary:**
- Use Keycloak + OAuth2/OIDC for secure, standards-based authentication.
- Use bearer tokens (JWT) for API access.
- Manage users/roles in Keycloak; enforce business logic in your backend.
- Avoid custom authentication logic for better security and maintainability.


---

## 11. Comparison: Custom JWT Authentication vs. Keycloak OAuth2/OIDC

### Your Current Approach (Custom JWT Auth)
- Users log in via a custom endpoint (e.g., `/login`).
- Backend verifies credentials and issues a JWT signed with HS256 (symmetric secret key).
- Token contains user info and roles, but is not standards-based.
- Token validation and user management are handled entirely by your backend.
- No Single Sign-On (SSO), social login, or centralized user management.
- Security depends on your implementation and secret management.

### Keycloak OAuth2/OIDC Approach (Recommended)
- Users are redirected to Keycloak for login (OIDC flow).
- Keycloak issues a JWT access token signed with RS256 (asymmetric key pair).
- Spring Boot validates tokens using Keycloak's public key (auto-fetched).
- Centralized user, role, and permission management in Keycloak.
- Supports SSO, social login, password reset, MFA, and more out of the box.
- Follows industry standards (OAuth2, OIDC, JWT best practices).

### Benefits of Using Keycloak OAuth2/OIDC
- **Security:** Stronger, standards-based authentication and token validation.
- **Centralized Management:** Manage users, roles, and permissions in one place.
- **Scalability:** Easily add new apps, clients, or login methods (Google, SAML, etc.).
- **Maintainability:** No need to maintain custom auth code or handle password storage.
- **Compliance:** Easier to meet security and privacy requirements.
- **Extensibility:** Add SSO, MFA, social login, and more with minimal changes.

**Recommendation:**
Migrate from custom JWT authentication to Keycloak OAuth2/OIDC for improved security, maintainability, and scalability.
