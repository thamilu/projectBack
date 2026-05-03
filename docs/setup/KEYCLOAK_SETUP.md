# Keycloak Setup and Configuration Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Initial Configuration](#initial-configuration)
4. [Realm Setup](#realm-setup)
5. [Client Configuration](#client-configuration)
6. [Role Configuration](#role-configuration)
7. [User Management](#user-management)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- Docker and Docker Compose (recommended)
- OR Standalone Keycloak installation
- PostgreSQL database
- Java 17+ (for backend integration)

### Required Knowledge
- Basic understanding of OAuth2/OIDC
- Docker basics
- REST API concepts

---

## Installation

### Option 1: Docker Compose (Recommended)

The project includes Keycloak in `docker-compose.yml`:

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  container_name: keycloak
  environment:
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
    KC_DB_USERNAME: keycloak
    KC_DB_PASSWORD: keycloak
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_HOSTNAME: localhost
    KC_HTTP_ENABLED: true
  ports:
    - "8080:8080"
  command:
    - start-dev
    - --import-realm
  volumes:
    - ./realm-export.json:/opt/keycloak/data/import/realm-export.json
  depends_on:
    - postgres
  networks:
    - eshop-network
```

**Start Keycloak:**
```bash
cd G:\Project\eshop_back
docker-compose up -d keycloak
```

**Verify:**
```bash
# Check if Keycloak is running
docker ps | grep keycloak

# Check logs
docker logs keycloak

# Access admin console
# URL: http://localhost:8080
# Username: admin
# Password: admin
```

### Option 2: Standalone Installation

Download from: https://www.keycloak.org/downloads

```bash
# Extract
unzip keycloak-23.0.zip
cd keycloak-23.0

# Set admin credentials
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=admin

# Start in dev mode
./bin/kc.sh start-dev
```

---

## Initial Configuration

### 1. Access Admin Console

**URL:** http://localhost:8080

**Default Credentials:**
- Username: `admin`
- Password: `admin`

⚠️ **Important:** Change the admin password in production!

### 2. Create Admin User (if needed)

If accessing for the first time:
1. Navigate to http://localhost:8080
2. Click "Administration Console"
3. Create admin credentials
4. Login

---

## Realm Setup

### Automatic Setup (Recommended)

The project includes a pre-configured realm export file: `realm-export.json`

**Import the realm:**

```bash
# Using Docker
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json \
  --override true

# OR restart with import flag (already configured in docker-compose)
docker-compose restart keycloak
```

**Verify import:**
1. Login to admin console
2. Top-left dropdown should show "eshop" realm
3. Verify roles exist: Customer, Seller, DELIVERY_AGENT

### Manual Realm Creation

If you need to create from scratch:

1. **Create Realm**
   - Admin Console → Click "master" dropdown (top-left)
   - Click "Create Realm"
   - Realm name: `eshop`
   - Enabled: ON
   - Click "Create"

2. **Configure Realm Settings**
   - Select "eshop" realm
   - Go to: Realm Settings

3. **General Settings**
   ```
   Display name: E-Shop
   HTML Display name: <b>E-Shop</b>
   Frontend URL: (leave empty for dev)
   Require SSL: None (for dev), External requests (for prod)
   ```

4. **Login Settings**
   - Go to: Realm Settings → Login
   - Configure:
     ```
     User registration: ON
     Forgot password: ON
     Remember me: ON
     Verify email: OFF (for dev), ON (for prod)
     Login with email: ON
     Duplicate emails: OFF
     ```

5. **User Registration Settings**
   - Go to: Realm Settings → User Registration
   - Default Roles: Add "Customer"
   - This ensures new users automatically get CUSTOMER role

6. **Themes (Optional)**
   - Login theme: keycloak
   - Account theme: keycloak
   - Admin theme: keycloak
   - Email theme: keycloak

7. **Tokens**
   - Go to: Realm Settings → Tokens
   - Configure:
     ```
     Access Token Lifespan: 5 minutes
     SSO Session Idle: 30 minutes
     SSO Session Max: 10 hours
     Refresh Token Max Reuse: 0
     ```

---

## Client Configuration

### Client 1: Frontend Client (Public)

**Purpose:** For frontend application (Next.js)

1. **Create Client**
   - Clients → Create client
   - Client ID: `eshop-client`
   - Client type: OpenID Connect
   - Click "Next"

2. **Capability config**
   ```
   Client authentication: OFF (public client)
   Authorization: OFF
   Standard flow: ON (OAuth2 Authorization Code)
   Direct access grants: OFF (use standard flow)
   Implicit flow: OFF (deprecated)
   Service accounts: OFF
   ```

3. **Access Settings**
   ```
   Root URL: http://localhost:3000
   Home URL: http://localhost:3000
   Valid redirect URIs: 
     - http://localhost:3000/*
     - http://localhost:4200/*
     - http://localhost:5173/*
   Valid post logout redirect URIs: 
     - http://localhost:3000/*
   Web origins: 
     - http://localhost:3000
     - http://localhost:4200
     - http://localhost:5173
   ```

4. **Advanced Settings**
   ```
   PKCE: S256 (required)
   Frontchannel logout: ON
   ```

### Client 2: Backend Service Account (Confidential)

**Purpose:** For backend to manage users/roles via Keycloak Admin API

1. **Create Client**
   - Clients → Create client
   - Client ID: `eshop-backend`
   - Client type: OpenID Connect
   - Click "Next"

2. **Capability config**
   ```
   Client authentication: ON (confidential)
   Authorization: OFF
   Standard flow: OFF
   Direct access grants: OFF
   Implicit flow: OFF
   Service accounts: ON
   ```

3. **Credentials**
   - Go to: Clients → eshop-backend → Credentials
   - Client Authenticator: Client Id and Secret
   - Copy the secret (e.g., `aWHhjsbAeg8LeeTvtkDerrCQGhEuJ5ph`)
   - Save this in backend `application.properties`

4. **Service Account Roles**
   - Go to: Clients → eshop-backend → Service Account Roles
   - Click "Assign role"
   - Filter by clients: Select "realm-management"
   - Assign these roles:
     - `manage-users`
     - `view-users`
     - `manage-realm`
     - `view-realm`

   This allows the backend to assign roles to users.

### Client Scopes and Mappers

**Ensure roles are included in JWT:**

1. **Go to:** Clients → eshop-client → Client Scopes

2. **Add roles mapper:**
   - Click "eshop-client-dedicated" scope
   - Go to "Mappers" tab
   - Click "Add mapper" → "By configuration"
   - Select "User Realm Role"
   
   Configure:
   ```
   Name: realm roles
   Mapper Type: User Realm Role
   Token Claim Name: realm_access.roles
   Claim JSON Type: String
   Add to ID token: ON
   Add to access token: ON
   Add to userinfo: ON
   Multivalued: ON
   ```

3. **Add username mapper:**
   - Add mapper → "User Property"
   ```
   Name: username
   Mapper Type: User Property
   Property: username
   Token Claim Name: preferred_username
   Claim JSON Type: String
   Add to ID token: ON
   Add to access token: ON
   Add to userinfo: ON
   ```

---

## Role Configuration

### Create Realm Roles

1. **Navigate to Roles**
   - Select "eshop" realm
   - Go to: Realm roles

2. **Create CUSTOMER Role**
   - Click "Create role"
   - Role name: `Customer`
   - Description: `Customer role for browsing and purchasing`
   - Click "Save"

3. **Create SELLER Role**
   - Click "Create role"
   - Role name: `Seller`
   - Description: `Seller role for managing products and shops`
   - Click "Save"

4. **Create DELIVERY_AGENT Role**
   - Click "Create role"
   - Role name: `DELIVERY_AGENT`
   - Description: `Delivery agent role for managing deliveries`
   - Click "Save"

5. **Create ADMIN Role** (Optional)
   - Click "Create role"
   - Role name: `ADMIN`
   - Description: `Administrator role with full access`
   - Click "Save"

### Set Default Role (IMPORTANT)

This ensures all new users get CUSTOMER role automatically:

1. **Go to:** Realm Settings → User Registration
2. **Default Roles section**
3. Click "Assign role"
4. Select "Customer"
5. Click "Assign"
6. **Save**

**Verify:**
- Go to: Realm Settings → User Registration
- Default Roles should show: `Customer`

---

## User Management

### Create Test Users

**User 1: Regular Customer**

1. Go to: Users → Create user
2. Configure:
   ```
   Username: customer
   Email: customer@eshop.com
   Email verified: ON (for dev)
   First name: Regular
   Last name: Customer
   Enabled: ON
   ```
3. Click "Create"
4. Go to: Credentials tab
   - Click "Set password"
   - Password: `customer`
   - Temporary: OFF
   - Click "Save"
5. Go to: Role mappings tab
   - Verify "Customer" role is assigned (should be automatic)

**User 2: Seller**

1. Create user with:
   ```
   Username: seller
   Email: seller@eshop.com
   First name: Seller
   Last name: User
   Password: seller
   ```
2. Assign roles:
   - Customer (should be automatic)
   - Seller (assign manually)

**User 3: Admin**

1. Create user with:
   ```
   Username: admin
   Email: admin@eshop.com
   First name: Admin
   Last name: User
   Password: admin
   ```
2. Assign roles:
   - Customer
   - Seller
   - ADMIN

### User Attributes (Optional)

You can add custom attributes to users:

1. Go to: Users → Select user → Attributes
2. Add custom attributes:
   - Key: `phone`
   - Value: `+1234567890`
3. Click "Save"

These attributes will be included in JWT if you create a mapper for them.

---

## Testing

### Test 1: Verify Keycloak is Running

```bash
# Check health
curl http://localhost:8080/health

# Get OpenID configuration
curl http://localhost:8080/realms/eshop/.well-known/openid-configuration | jq
```

### Test 2: Test User Login

```bash
# Login with password grant (requires direct access grants enabled)
curl -X POST http://localhost:8080/realms/eshop/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=eshop-client" \
  -d "username=customer" \
  -d "password=customer" \
  -d "scope=openid profile email" | jq

# Response includes:
# - access_token
# - refresh_token
# - id_token
```

### Test 3: Verify Roles in JWT

```bash
# Decode access token (copy from above response)
echo "<access_token>" | cut -d. -f2 | base64 -d | jq

# Should include:
# {
#   "realm_access": {
#     "roles": ["Customer"]
#   },
#   "preferred_username": "customer",
#   ...
# }
```

### Test 4: Test Service Account

```bash
# Get service account token
curl -X POST http://localhost:8080/realms/eshop/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=eshop-backend" \
  -d "client_secret=aWHhjsbAeg8LeeTvtkDerrCQGhEuJ5ph" | jq

# Use token to list users
TOKEN="<service_account_token>"
curl http://localhost:8080/admin/realms/eshop/users \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Test 5: Test Role Assignment

```bash
# Get user ID
USER_ID=$(curl http://localhost:8080/admin/realms/eshop/users?username=customer \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Get Seller role
ROLE=$(curl http://localhost:8080/admin/realms/eshop/roles/Seller \
  -H "Authorization: Bearer $TOKEN")

# Assign Seller role to customer
curl -X POST http://localhost:8080/admin/realms/eshop/users/$USER_ID/role-mappings/realm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "[$ROLE]"

# Verify
curl http://localhost:8080/admin/realms/eshop/users/$USER_ID/role-mappings/realm \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Troubleshooting

### Issue: Cannot Access Admin Console

**Symptoms:** http://localhost:8080 not accessible

**Solutions:**

1. **Check if Keycloak is running:**
   ```bash
   docker ps | grep keycloak
   ```

2. **Check logs:**
   ```bash
   docker logs keycloak
   ```

3. **Check port binding:**
   ```bash
   netstat -an | grep 8080
   # or
   Get-NetTCPConnection -LocalPort 8080
   ```

4. **Restart Keycloak:**
   ```bash
   docker-compose restart keycloak
   ```

### Issue: Realm Not Found

**Symptoms:** Frontend redirects fail, "Realm not found" error

**Solutions:**

1. **Verify realm exists:**
   ```bash
   curl http://localhost:8080/realms/eshop/.well-known/openid-configuration
   ```

2. **Check realm name in configuration:**
   - Backend `application.properties`: `keycloak.realm=eshop`
   - Frontend `.env`: `KEYCLOAK_REALM=eshop`

3. **Re-import realm:**
   ```bash
   docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
     --file /opt/keycloak/data/import/realm-export.json \
     --override true
   ```

### Issue: Client Not Found

**Symptoms:** "Client not found" during login

**Solutions:**

1. **Verify client exists:**
   - Admin Console → Clients
   - Check for "eshop-client" and "eshop-backend"

2. **Check client ID in code:**
   - Frontend: `KEYCLOAK_CLIENT_ID=eshop-client`
   - Backend: `keycloak.resource=eshop-backend`

3. **Recreate client** if missing (follow Client Configuration section)

### Issue: Invalid Redirect URI

**Symptoms:** "Invalid redirect_uri" error during login

**Solutions:**

1. **Check Valid Redirect URIs in Keycloak:**
   - Clients → eshop-client → Settings
   - Valid redirect URIs should include:
     - `http://localhost:3000/*`
     - Your frontend URL with wildcard

2. **Check redirect_uri in frontend request:**
   - Must exactly match one of the configured URIs

3. **Common mistake:** Missing trailing `/*` in Keycloak configuration

### Issue: Roles Not in JWT

**Symptoms:** JWT doesn't include `realm_access.roles`

**Solutions:**

1. **Check client scope mappers:**
   - Clients → eshop-client → Client Scopes → eshop-client-dedicated → Mappers
   - Ensure "realm roles" mapper exists

2. **Add roles mapper** (see Client Configuration section)

3. **Request correct scopes:**
   ```
   scope=openid profile email
   ```

### Issue: Service Account Can't Manage Users

**Symptoms:** 403 Forbidden when assigning roles

**Solutions:**

1. **Check service account roles:**
   - Clients → eshop-backend → Service Account Roles
   - Must have: `manage-users`, `view-users`, `manage-realm`

2. **Assign missing roles:**
   - Filter by clients: realm-management
   - Assign required roles

3. **Verify client secret:**
   - Check `application.properties`: `keycloak.credentials.secret`
   - Must match: Clients → eshop-backend → Credentials

### Issue: Default Role Not Assigned

**Symptoms:** New users don't have Customer role

**Solutions:**

1. **Check default roles configuration:**
   - Realm Settings → User Registration → Default Roles
   - "Customer" should be listed

2. **Add default role:**
   - Click "Assign role"
   - Select "Customer"
   - Save

3. **Test with new user:**
   - Register new user
   - Check Role Mappings tab
   - Should have "Customer" role

---

## Production Considerations

### Security

1. **Change Admin Password**
   ```bash
   # Via Admin Console
   # Or environment variable
   KEYCLOAK_ADMIN_PASSWORD=<strong-password>
   ```

2. **Enable HTTPS**
   ```yaml
   # docker-compose.yml
   KC_HTTPS_ENABLED: true
   KC_HTTPS_CERTIFICATE_FILE: /path/to/cert.pem
   KC_HTTPS_CERTIFICATE_KEY_FILE: /path/to/key.pem
   ```

3. **Enable Email Verification**
   - Realm Settings → Login → Verify email: ON
   - Configure SMTP settings

4. **Configure SMTP**
   - Realm Settings → Email
   ```
   From: noreply@yourdomain.com
   SMTP Host: smtp.gmail.com
   SMTP Port: 587
   Enable StartTLS: ON
   Enable Authentication: ON
   Username: your-email@gmail.com
   Password: app-specific-password
   ```

5. **Enable SSL Required**
   - Realm Settings → General
   - Require SSL: External requests

### Database

1. **Use External PostgreSQL**
   ```yaml
   KC_DB_URL: jdbc:postgresql://production-db:5432/keycloak
   KC_DB_USERNAME: keycloak_user
   KC_DB_PASSWORD: <strong-password>
   ```

2. **Database Backups**
   ```bash
   # Backup Keycloak database
   pg_dump -h localhost -U keycloak_user keycloak > keycloak_backup.sql
   ```

### Scaling

1. **Clustered Setup**
   - Use external cache (Infinispan)
   - Configure load balancer
   - Shared database

2. **Performance Tuning**
   ```
   KC_DB_POOL_INITIAL_SIZE: 10
   KC_DB_POOL_MAX_SIZE: 50
   ```

### Monitoring

1. **Health Checks**
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/metrics
   ```

2. **Logging**
   ```bash
   # Configure log level
   KC_LOG_LEVEL: INFO
   ```

---

## Configuration Files Reference

### Backend: application.properties

```properties
# Keycloak Configuration
keycloak.auth-server-url=http://localhost:8080
keycloak.realm=eshop
keycloak.resource=eshop-backend
keycloak.credentials.secret=aWHhjsbAeg8LeeTvtkDerrCQGhEuJ5ph

# For admin operations
keycloak.admin.clientId=eshop-backend
keycloak.admin.username=admin
keycloak.admin.password=admin
```

### Frontend: .env

```env
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=eshop
KEYCLOAK_CLIENT_ID=eshop-client
NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8080
NEXT_PUBLIC_KEYCLOAK_REALM=eshop
```

### Docker Compose: docker-compose.yml

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  environment:
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
    KC_DB_USERNAME: keycloak
    KC_DB_PASSWORD: keycloak
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_HOSTNAME: localhost
    KC_HTTP_ENABLED: true
  ports:
    - "8080:8080"
  command:
    - start-dev
    - --import-realm
  volumes:
    - ./realm-export.json:/opt/keycloak/data/import/realm-export.json
```

---

## Quick Reference Commands

```bash
# Start Keycloak
docker-compose up -d keycloak

# Stop Keycloak
docker-compose stop keycloak

# View logs
docker logs -f keycloak

# Access bash
docker exec -it keycloak bash

# Import realm
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json

# Export realm
docker exec -it keycloak /opt/keycloak/bin/kc.sh export \
  --realm eshop \
  --file /tmp/realm-export.json

# Copy exported realm
docker cp keycloak:/tmp/realm-export.json ./realm-export.json

# Restart Keycloak
docker-compose restart keycloak

# Remove Keycloak (data will be lost)
docker-compose down keycloak
docker volume rm eshop_keycloak_data
```

---

## Next Steps

After completing Keycloak setup:

1. ✅ Configure backend to use Keycloak (see `application.properties`)
2. ✅ Configure frontend OAuth2 flow
3. ✅ Test user registration and login
4. ✅ Test role assignment
5. ✅ Review [Role Management Guide](../guides/ROLE_MANAGEMENT.md)

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-17  
**Author:** Development Team
