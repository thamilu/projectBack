# Keycloak + Spring Boot Integration Guide (with Docker)

## 1. Run Keycloak with Docker

Create a file named `keycloak-docker-compose.yml` with the following content:

```yaml
version: '3.8'
services:
  keycloak:
    image: quay.io/keycloak/keycloak:24.0.0
    command: start-dev --http-port=8080
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
```

Start Keycloak:
```sh
docker compose -f keycloak-docker-compose.yml up
```

---

## 2. Create the 'eshop' Realm in Keycloak

1. Open [http://localhost:8080](http://localhost:8080) and log in as `admin`/`admin`.
2. In the admin console, click the realm dropdown (top left), then click **Create realm**.
3. Enter `eshop` as the realm name and click **Create**.

---

## 3. Create a Client for Your App

1. In the 'eshop' realm, go to **Clients** > **Create client**.
2. Set `Client ID` to `eshop-client`.
3. Set **Client Protocol** to `openid-connect`.
4. Set **Access Type** to `public` (or `confidential` if you want to use a client secret).
5. Enable **Direct Access Grants** (for password grant testing).
6. Save.

---

## 4. Create a Test User

1. In the 'eshop' realm, go to **Users** > **Add user**.
2. Fill in username and other details, then save.
3. Go to **Credentials** tab, set a password, and turn off "Temporary".
4. Assign roles if your API requires them.

---

## 5. Configure Spring Boot

In your `application.properties`:
```
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/eshop
```

No need to set the public key manually—Spring Boot will fetch it from Keycloak.

---

## 6. Get a Token and Test with Postman

### Get Token
- POST to: `http://localhost:8080/realms/eshop/protocol/openid-connect/token`
- Body (x-www-form-urlencoded):
    - grant_type: password
    - client_id: eshop-client
    - username: <your-user>
    - password: <your-password>
    - client_secret: <if confidential client>

Copy the `access_token` from the response.

### Call Your API
- Set `Authorization: Bearer <access_token>` header in Postman.
- POST to your API endpoint (e.g., `http://localhost:8082/api/v1/products`).
- Provide the required JSON body for product creation.

---

## 7. Troubleshooting
- If you get `JWT invalid` or `Another algorithm expected`, make sure:
    - You use a Keycloak-issued token (not a hardcoded or third-party token).
    - The client uses RS256 (default in Keycloak).
    - The token is for the correct realm (`eshop`).
- If you get 401/403, check user roles and token validity.

---

## 8. Useful Endpoints
- OpenID config: [http://localhost:8080/realms/eshop/.well-known/openid-configuration](http://localhost:8080/realms/eshop/.well-known/openid-configuration)
- JWKS (public keys): [http://localhost:8080/realms/eshop/protocol/openid-connect/certs](http://localhost:8080/realms/eshop/protocol/openid-connect/certs)

---

**This document summarizes all steps to set up Keycloak with Docker, configure a realm, client, user, and test Spring Boot API authentication.**
