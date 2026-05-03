# 🔐 Keycloak OAuth2 Setup Guide - EShop Application

## 📋 Overview

This guide provides step-by-step instructions for setting up Keycloak OAuth2 authentication for the EShop application in development mode.

---

## 🎯 Prerequisites

- Docker Desktop installed and running
- Java 21+ installed
- PostgreSQL 15+ (for Keycloak database)
- EShop application source code

---

## 🚀 Quick Start (Dev Mode)

### 1️⃣ Start Keycloak with Docker Compose

```bash
# Navigate to project root
cd f:/MyprojectAgent/EcomApp/eshop

# Start Keycloak and PostgreSQL
docker compose -f docker-compose.keycloak.yml up -d

# Check status
docker compose -f docker-compose.keycloak.yml ps

# View logs
docker compose -f docker-compose.keycloak.yml logs -f keycloak
```

**Access Keycloak:**
- URL: http://localhost:8081
- Admin Username: `admin`
- Admin Password: `admin`

---

## 🔧 Keycloak Configuration

### 2️⃣ Create Realm

1. Login to Keycloak Admin Console (http://localhost:8081)
2. Click dropdown in top-left (currently shows "Master")
3. Click "Create Realm"
4. Enter realm name: `eshop-dev`
5. Click "Create"

### 3️⃣ Create Client

1. In `eshop-dev` realm, go to **Clients** → **Create client**

**General Settings:**
- Client ID: `eshop-backend`
- Name: `EShop Backend API`
- Description: `EShop REST API OAuth2 Client`
- Click "Next"

**Capability config:**
- Client authentication: `ON`
- Authorization: `OFF`
- Authentication flow:
  - ✅ Standard flow
  - ✅ Direct access grants
  - ✅ Service accounts roles
- Click "Next"

**Login settings:**
- Root URL: `http://localhost:8082`
- Home URL: `http://localhost:8082`
- Valid redirect URIs: 
  - `http://localhost:8082/*`
  - `http://localhost:3000/*`
  - `http://localhost:4200/*`
- Valid post logout redirect URIs: `+`
- Web origins: 
  - `http://localhost:8082`
  - `http://localhost:3000`
  - `http://localhost:4200`

Click "Save"

### 4️⃣ Create Realm Roles

1. Go to **Realm roles** → **Create role**

Create the following roles:

| Role Name | Description |
|-----------|-------------|
| `ADMIN` | System administrator with full access |
| `SELLER` | Shop owner with product/order management |
| `CUSTOMER` | Customer with order/account access |
| `DELIVERY_AGENT` | Delivery personnel with delivery management |

For each role:
- Role name: (as above)
- Description: (as above)
- Click "Save"

### 5️⃣ Create Client Scope for Roles

1. Go to **Client scopes** → **Create client scope**

**Settings:**
- Name: `roles`
- Description: `User roles for authorization`
- Type: `Default`
- Protocol: `OpenID Connect`
- Display on consent screen: `OFF`
- Include in token scope: `ON`
- Click "Save"

2. Go to **Mappers** tab → **Add mapper** → **By configuration** → **User Realm Role**

**Mapper Settings:**
- Name: `realm-roles`
- Mapper type: `User Realm Role`
- Multivalued: `ON`
- Token Claim Name: `roles`
- Claim JSON Type: `String`
- Add to ID token: `ON`
- Add to access token: `ON`
- Add to userinfo: `ON`
- Click "Save"

### 6️⃣ Assign Scope to Client

1. Go to **Clients** → **eshop-backend** → **Client scopes** tab
2. Click "Add client scope"
3. Select `roles` scope
4. Select "Default" type
5. Click "Add"

### 7️⃣ Create Test Users

Go to **Users** → **Add user**

#### Admin User
- Username: `admin`
- Email: `admin@eshop.com`
- First name: `Admin`
- Last name: `User`
- Email verified: `ON`
- Click "Create"

**Set Password:**
- Go to **Credentials** tab
- Set password: `admin123`
- Temporary: `OFF`
- Click "Set password"

**Assign Roles:**
- Go to **Role mappings** tab
- Click "Assign role"
- Select `ADMIN`
- Click "Assign"

#### Seller User
Repeat above steps:
- Username: `seller1`
- Email: `seller1@eshop.com`
- Password: `seller123`
- Role: `SELLER`

#### Customer User
- Username: `customer1`
- Email: `customer1@eshop.com`
- Password: `customer123`
- Role: `CUSTOMER`

#### Delivery Agent User
- Username: `delivery1`
- Email: `delivery1@eshop.com`
- Password: `delivery123`
- Role: `DELIVERY_AGENT`

---

## 🔑 Client Credentials (For Application)

1. Go to **Clients** → **eshop-backend** → **Credentials** tab
2. Copy the **Client secret**
3. Note: You don't need to add this to application.properties (JWT validation uses public keys)

---

## ⚙️ Application Configuration

### application-dev.properties

```properties
# Enable Keycloak
security.keycloak.enabled=true
security.keycloak.realm=eshop-dev
security.keycloak.auth-server-url=http://localhost:8081

# OAuth2 Resource Server
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/eshop-dev
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/eshop-dev/protocol/openid-connect/certs

# JWT Configuration
app.security.jwt.authority-prefix=ROLE_
app.security.jwt.authorities-claim-name=roles
```

---

## 🧪 Testing Authentication

### Option 1: Swagger UI

1. Start EShop application:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

2. Open Swagger UI: http://localhost:8082/swagger-ui.html

3. Click **Authorize** button

4. In OAuth2 modal:
   - Enter credentials: `admin` / `admin123`
   - Click "Authorize"
   - Click "Close"

5. Try any protected endpoint (e.g., `/api/v1/dashboard/admin`)

### Option 2: cURL (Direct Access Grant)

```bash
# Get access token
TOKEN=$(curl -X POST 'http://localhost:8081/realms/eshop-dev/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'username=admin' \
  -d 'password=admin123' \
  -d 'grant_type=password' \
  -d 'client_id=eshop-backend' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  | jq -r '.access_token')

# Call protected API
curl -X GET 'http://localhost:8082/api/v1/dashboard/admin/statistics' \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### Option 3: Postman

1. Create new request
2. Authorization tab:
   - Type: **OAuth 2.0**
   - Grant Type: **Password Credentials**
   - Access Token URL: `http://localhost:8081/realms/eshop-dev/protocol/openid-connect/token`
   - Client ID: `eshop-backend`
   - Client Secret: (from Keycloak)
   - Username: `admin`
   - Password: `admin123`
3. Click "Get New Access Token"
4. Use token in request headers

---

## 🔍 Token Inspection

### Decode JWT Token

Visit: https://jwt.io

Paste your access token to see:

**Header:**
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "..."
}
```

**Payload:**
```json
{
  "exp": 1734284400,
  "iat": 1734280800,
  "jti": "...",
  "iss": "http://localhost:8081/realms/eshop-dev",
  "aud": "account",
  "sub": "...",
  "typ": "Bearer",
  "azp": "eshop-backend",
  "roles": ["ADMIN"],
  "email": "admin@eshop.com",
  "preferred_username": "admin"
}
```

### Verify Role Mapping

Ensure `roles` claim contains:
- `["ADMIN"]` for admin user
- `["SELLER"]` for seller user
- `["CUSTOMER"]` for customer user
- `["DELIVERY_AGENT"]` for delivery agent

---

## 🛠️ Troubleshooting

### Issue: "Invalid token issuer"

**Cause:** Issuer URI mismatch

**Solution:**
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/eshop-dev
```

Ensure Keycloak is accessible at this URL.

### Issue: "403 Forbidden - Access Denied"

**Cause:** Role mapping not working

**Checklist:**
1. ✅ Client scope `roles` created
2. ✅ Mapper `realm-roles` configured
3. ✅ Token claim name is `roles` (not `realm_access.roles`)
4. ✅ User has assigned role
5. ✅ Spring config has `app.security.jwt.authorities-claim-name=roles`

### Issue: "CORS Error"

**Solution:**

In `KeycloakSecurityConfig.java`, verify CORS config:
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "http://localhost:4200",
    "http://localhost:8082"
));
```

### Issue: Keycloak container won't start

**Check PostgreSQL:**
```bash
docker compose -f docker-compose.keycloak.yml logs postgres
```

**Restart services:**
```bash
docker compose -f docker-compose.keycloak.yml down
docker compose -f docker-compose.keycloak.yml up -d
```

---

## 📊 Keycloak Admin Tasks

### View Active Sessions

1. Go to **Realm settings** → **Sessions** tab
2. See active user sessions
3. Revoke sessions if needed

### View Events

1. Go to **Realm settings** → **Events** tab
2. Enable event logging
3. Monitor login attempts, token requests, errors

### Export Realm Configuration

```bash
docker exec -it eshop-keycloak-dev \
  /opt/keycloak/bin/kc.sh export \
  --dir /tmp/export \
  --realm eshop-dev
```

### Import Realm Configuration

```bash
docker exec -it eshop-keycloak-dev \
  /opt/keycloak/bin/kc.sh import \
  --file /tmp/export/eshop-dev-realm.json
```

---

## 🔒 Production Considerations

### ⚠️ DO NOT USE IN PRODUCTION AS-IS

This setup is for **DEVELOPMENT ONLY**. For production:

1. **Use proper database**: Not H2, use PostgreSQL/MySQL
2. **Enable HTTPS**: SSL/TLS for Keycloak
3. **Strong passwords**: Change default admin password
4. **Client secrets**: Rotate regularly, use environment variables
5. **Restrict CORS**: Specific origins, not wildcards
6. **Enable rate limiting**: Prevent brute force attacks
7. **Monitoring**: Enable metrics and logging
8. **Backup**: Regular database backups
9. **Update Keycloak**: Keep up-to-date with security patches
10. **Network security**: Firewall rules, VPC isolation

---

## 📚 Additional Resources

- **Keycloak Documentation**: https://www.keycloak.org/documentation
- **Spring Security OAuth2**: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- **JWT Specification**: https://datatracker.ietf.org/doc/html/rfc7519
- **OAuth 2.0 RFC**: https://datatracker.ietf.org/doc/html/rfc6749

---

## 👥 Support

**Team**: EShop Development Team  
**Email**: api-support@eshop.com  
**Documentation**: https://docs.eshop.com/security

---

## ✅ Setup Verification Checklist

- [ ] Keycloak running on http://localhost:8081
- [ ] Realm `eshop-dev` created
- [ ] Client `eshop-backend` configured
- [ ] Roles created: ADMIN, SELLER, CUSTOMER, DELIVERY_AGENT
- [ ] Client scope `roles` with mapper configured
- [ ] Test users created with passwords set
- [ ] Roles assigned to users
- [ ] Application configured with correct issuer URI
- [ ] Token obtained successfully via cURL/Postman
- [ ] Protected endpoint accessible with valid token
- [ ] Swagger UI authentication working
- [ ] Role-based access control verified

**Status**: ✅ **READY FOR DEVELOPMENT**
