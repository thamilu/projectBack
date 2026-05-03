# Role Management Guide

## Table of Contents
1. [Overview](#overview)
2. [Role Hierarchy](#role-hierarchy)
3. [Role Assignment Rules](#role-assignment-rules)
4. [Configuration](#configuration)
5. [User Journeys](#user-journeys)
6. [API Reference](#api-reference)
7. [Troubleshooting](#troubleshooting)

---

## Overview

The e-commerce application uses **Keycloak** for centralized user management and role-based access control (RBAC). All authentication, user registration, and role management are handled through Keycloak.

### Authentication Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────────┐
│   Frontend  │ ◄────► │   Keycloak   │ ◄────► │  Backend (API)  │
│  (Next.js)  │  OAuth2 │              │  JWT   │   (Spring Boot) │
└─────────────┘         └──────────────┘         └─────────────────┘
                               │
                               │ User DB
                               ▼
                        ┌──────────────┐
                        │  PostgreSQL  │
                        └──────────────┘
```

### Key Principles

- ✅ **Single Source of Truth**: Keycloak manages all users and roles
- ✅ **No Backend Registration**: Registration happens only in Keycloak
- ✅ **Role-Based Access**: All endpoints use `@PreAuthorize` annotations
- ✅ **Automatic Role Assignment**: CUSTOMER role assigned during registration
- ✅ **Admin Approval Required**: SELLER and DELIVERY_AGENT roles require approval

---

## Role Hierarchy

### Available Roles

| Role | Keycloak Name | Backend Constant | Auto-Assigned | Approval Required |
|------|---------------|------------------|---------------|-------------------|
| Customer | `Customer` | `Roles.CUSTOMER` | ✅ Yes | ❌ No |
| Seller | `Seller` | `Roles.SELLER` | ❌ No | ✅ Yes (Admin) |
| Delivery Agent | `DELIVERY_AGENT` | `Roles.DELIVERY_AGENT` | ❌ No | ✅ Yes (Admin) |
| Administrator | `ADMIN` | `Roles.ADMIN` | ❌ No | Manual |

### Role Capabilities

#### CUSTOMER Role
**Permissions:**
- Browse products and categories
- Add items to cart
- Place orders
- View order history
- Update profile
- Apply to become seller/delivery agent

**Endpoints:**
- `GET /api/v1/products/**`
- `POST /api/v1/cart/**`
- `POST /api/v1/orders/**`
- `GET /api/v1/me`

#### SELLER Role
**Permissions:**
- All CUSTOMER permissions
- Create/manage products
- Manage shop/store
- View seller dashboard
- Process orders

**Endpoints:**
- `POST /api/v1/sellers/register`
- `GET /api/v1/sellers/profile`
- `PUT /api/v1/sellers/profile`
- `GET /api/v1/dashboard/seller/**`
- `POST /api/v1/products`

#### DELIVERY_AGENT Role
**Permissions:**
- All CUSTOMER permissions
- View delivery assignments
- Update delivery status
- Manage delivery routes

**Endpoints:**
- `POST /api/v1/delivery/register`
- `GET /api/v1/delivery/assignments`
- `PUT /api/v1/delivery/status`

#### ADMIN Role
**Permissions:**
- All system permissions
- Approve/reject sellers
- Approve/reject delivery agents
- Manage users
- System configuration

**Endpoints:**
- `GET /api/v1/admin/approvals/**`
- `POST /api/v1/admin/approvals/**`
- `GET /api/v1/admin/users`

---

## Role Assignment Rules

### Automatic Assignment (CUSTOMER)

**When**: During user registration in Keycloak

**How**: Keycloak realm configured with `defaultRoles: ["Customer"]`

**Process:**
```
User submits registration form
    ↓
Keycloak creates user
    ↓
Keycloak auto-assigns "Customer" role (via default roles)
    ↓
User can immediately login and access customer endpoints
```

**Fallback**: If Keycloak default role fails, backend assigns CUSTOMER role on first login (JIT sync)

### Manual Assignment (SELLER)

**When**: After admin approval of seller application

**How**: Admin approves via API, backend assigns role using KeycloakService

**Process:**
```
Customer applies to become seller
    ↓
POST /api/v1/sellers/register
    ↓
SellerProfile created with status: PENDING
    ↓
Admin reviews application
    ↓
GET /api/v1/admin/approvals/sellers
    ↓
Admin approves
    ↓
POST /api/v1/admin/approvals/sellers/{id}/APPROVE
    ↓
SellerService.approveSeller() executes:
  - Updates status to ACTIVE
  - Calls KeycloakService.assignRoleByUsername(username, "SELLER")
    ↓
User now has both CUSTOMER and SELLER roles
```

### Manual Assignment (DELIVERY_AGENT)

**When**: After admin approval of delivery agent application

**Process**: Similar to SELLER role assignment

---

## Configuration

### Keycloak Realm Configuration

**File**: `realm-export.json`

```json
{
  "realm": "eshop",
  "enabled": true,
  "registrationAllowed": true,
  "registrationEmailAsUsername": false,
  "editUsernameAllowed": false,
  "resetPasswordAllowed": true,
  "defaultRoles": ["Customer"],
  "roles": {
    "realm": [
      {
        "name": "Customer",
        "description": "Customer role"
      },
      {
        "name": "Seller",
        "description": "Seller role"
      },
      {
        "name": "DELIVERY_AGENT",
        "description": "Delivery Agent role"
      }
    ]
  }
}
```

### Backend Configuration

**File**: `src/main/java/com/eshop/app/constants/Roles.java`

```java
public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String SELLER = "SELLER";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String DELIVERY_AGENT = "DELIVERY_AGENT";
}
```

### Security Configuration

**File**: `src/main/java/com/eshop/app/config/OAuth2SecurityConfig.java`

Example endpoint protection:
```java
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<?> getCart() { ... }

@PreAuthorize("hasRole('SELLER')")
public ResponseEntity<?> createProduct() { ... }

@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> approveSeller() { ... }
```

---

## User Journeys

### Journey 1: New Customer Registration

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: User Registration (Keycloak)                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
User navigates to: http://localhost:8080/realms/eshop/account
User clicks "Register"
User fills form:
  - Username: john_doe
  - Email: john@example.com
  - Password: ********
  - First Name: John
  - Last Name: Doe
User submits registration
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Keycloak Auto-Assigns CUSTOMER Role                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Keycloak creates user account
Keycloak assigns "Customer" role automatically (defaultRoles)
User receives confirmation email (if configured)
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: User Logs In                                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
User navigates to frontend (e.g., http://localhost:3000)
User enters credentials and logs in
Keycloak redirects with authorization code
Frontend exchanges code for JWT tokens
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: Access Customer Features                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Frontend calls: GET /api/v1/me
Response includes: "roles": ["CUSTOMER"]
User can now:
  - Browse products
  - Add to cart
  - Place orders
  - View order history
```

### Journey 2: Customer Becomes Seller

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Customer Applies to Become Seller                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Customer (logged in) navigates to "Become a Seller"
Customer fills seller registration form:
  - Business Name
  - Business Type (FARMER/WHOLESALER/RETAILER)
  - Contact Details
  - Tax ID (if applicable)
  - Bank Details
  - Accepts Terms & Conditions
                              ↓
Frontend calls: POST /api/v1/sellers/register
Request body:
{
  "displayName": "Green Valley Farm",
  "businessTypes": ["FARMER"],
  "email": "contact@greenvalley.com",
  "phone": "+919876543210",
  "acceptedTerms": true,
  ...
}
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Backend Creates Seller Profile (PENDING)               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
SellerService.registerSeller() executes:
  - Validates terms acceptance
  - Creates SellerProfile entity
  - Sets status: PENDING
  - Saves to database
  
Response:
{
  "status": "success",
  "data": {
    "id": 1,
    "status": "PENDING",
    "displayName": "Green Valley Farm",
    ...
  }
}
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: Admin Reviews Application                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Admin logs in with ADMIN role
Admin calls: GET /api/v1/admin/approvals/sellers
Response shows pending applications:
[
  {
    "id": 1,
    "displayName": "Green Valley Farm",
    "status": "PENDING",
    "userId": 123,
    ...
  }
]
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: Admin Approves Seller                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Admin reviews application details
Admin decides to approve
Admin calls: POST /api/v1/admin/approvals/sellers/1/APPROVE
                              ↓
SellerService.approveSeller() executes:
  1. Updates SellerProfile:
     - status = ACTIVE
     - approvedBy = "admin"
     - approvedAt = current timestamp
  2. Assigns SELLER role in Keycloak:
     - Calls KeycloakService.assignRoleByUsername(username, "SELLER")
     - Keycloak adds "Seller" role to user
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 5: User Now Has SELLER Role                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
User logs in again (or refreshes token)
JWT now includes: "roles": ["CUSTOMER", "SELLER"]
User can now access:
  - Seller dashboard
  - Product management
  - Order management
  - Shop settings
```

### Journey 3: Admin Rejects Seller Application

```
Admin calls: POST /api/v1/admin/approvals/sellers/1/REJECT

SellerService.rejectSeller() executes:
  1. Updates SellerProfile:
     - status = REJECTED
     - rejectedBy = "admin"
     - rejectedAt = current timestamp
     - rejectionReason = "Incomplete documentation"
  2. Does NOT assign SELLER role

User remains with CUSTOMER role only
User can re-apply after addressing rejection reasons
```

---

## API Reference

### Customer Endpoints

#### Get Current User Info
```http
GET /api/v1/me
Authorization: Bearer {jwt-token}

Response:
{
  "sub": "uuid-here",
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["CUSTOMER"],
  ...
}
```

### Seller Endpoints

#### Register as Seller
```http
POST /api/v1/sellers/register
Authorization: Bearer {customer-jwt-token}
Content-Type: application/json

{
  "identityType": "INDIVIDUAL",
  "businessTypes": ["FARMER"],
  "displayName": "Green Valley Farm",
  "businessName": "Green Valley Organic Farms Pvt Ltd",
  "email": "contact@greenvalley.com",
  "phone": "+919876543210",
  "description": "Organic vegetables and fruits",
  "acceptedTerms": true,
  "aadhar": "123456789012",
  "farmLocationVillage": "Pune",
  "landArea": "10 acres"
}

Response (201 Created):
{
  "status": "success",
  "message": "Seller profile registered successfully",
  "data": {
    "id": 1,
    "userId": 123,
    "status": "PENDING",
    "displayName": "Green Valley Farm",
    ...
  }
}
```

#### Get Seller Profile
```http
GET /api/v1/sellers/profile
Authorization: Bearer {seller-jwt-token}

Response:
{
  "id": 1,
  "userId": 123,
  "status": "ACTIVE",
  "displayName": "Green Valley Farm",
  ...
}
```

### Admin Endpoints

#### Get Pending Sellers
```http
GET /api/v1/admin/approvals/sellers
Authorization: Bearer {admin-jwt-token}

Response:
[
  {
    "id": 1,
    "userId": 123,
    "status": "PENDING",
    "displayName": "Green Valley Farm",
    "createdAt": "2026-02-17T10:30:00Z",
    ...
  }
]
```

#### Approve Seller
```http
POST /api/v1/admin/approvals/sellers/1/APPROVE
Authorization: Bearer {admin-jwt-token}

Response (200 OK):
{
  "message": "Seller approved successfully"
}
```

#### Reject Seller
```http
POST /api/v1/admin/approvals/sellers/1/REJECT
Authorization: Bearer {admin-jwt-token}
Content-Type: application/json

{
  "reason": "Incomplete documentation"
}

Response (200 OK):
{
  "message": "Seller rejected"
}
```

---

## Troubleshooting

### Issue 1: New User Doesn't Have CUSTOMER Role

**Symptoms:**
- User can register and login
- API calls return 403 Forbidden
- JWT doesn't include CUSTOMER role

**Diagnosis:**
```bash
# Check user roles in Keycloak
curl http://localhost:8080/admin/realms/eshop/users/{userId}/role-mappings/realm \
  -H "Authorization: Bearer {admin-token}"
```

**Solutions:**

1. **Check Keycloak default roles configuration:**
   ```bash
   # Via API
   curl http://localhost:8080/admin/realms/eshop \
     -H "Authorization: Bearer {admin-token}" | jq '.defaultRoles'
   
   # Should return: ["Customer"]
   ```

2. **Via Keycloak Admin Console:**
   - Login: http://localhost:8080
   - Realm: eshop
   - Realm Settings → User Registration → Default Roles
   - Verify "Customer" is listed

3. **Manually assign role:**
   - Keycloak Admin Console
   - Users → Select user → Role Mappings
   - Add "Customer" role

4. **Backend safety net:**
   - The application has a fallback mechanism
   - User logs in → Backend checks for CUSTOMER role
   - If missing, assigns it automatically (see `SellerService.resolveUserId()`)

### Issue 2: Seller Application Auto-Approved (Should Be PENDING)

**Symptoms:**
- Seller application status shows ACTIVE immediately
- SELLER role assigned without admin approval

**Diagnosis:**
Check code in `SellerService.registerSeller()` around line 93-108

**Solution:**
Ensure auto-approval code is removed:
```java
// WRONG (old code):
approveSeller(saved.getId(), "SYSTEM (Auto-Approve)");

// CORRECT (new code):
return toResponse(saved);
```

### Issue 3: Admin Can't Approve Seller

**Symptoms:**
- Admin calls approve endpoint
- Returns error or seller stays PENDING
- SELLER role not assigned in Keycloak

**Diagnosis:**
1. Check logs for errors during role assignment
2. Verify user has `keycloakId` set in database
3. Check Keycloak service account permissions

**Solutions:**

1. **Check user's Keycloak ID:**
   ```sql
   SELECT id, username, email, keycloak_id FROM users WHERE id = 123;
   ```
   
   If `keycloak_id` is NULL, sync it:
   ```java
   // This happens automatically on next login
   // Or manually update via admin panel
   ```

2. **Verify Keycloak service account permissions:**
   - Keycloak Admin Console
   - Clients → eshop-backend → Service Account Roles
   - Ensure it has:
     - `manage-users`
     - `manage-realm`
     - `view-users`

3. **Check KeycloakService configuration:**
   ```properties
   # application.properties
   keycloak.auth-server-url=http://localhost:8080
   keycloak.realm=eshop
   keycloak.resource=eshop-backend
   keycloak.credentials.secret=your-client-secret
   ```

### Issue 4: JWT Doesn't Include Roles

**Symptoms:**
- User has roles in Keycloak
- JWT token doesn't include roles in claims
- Backend can't validate permissions

**Diagnosis:**
```bash
# Decode JWT token
echo "{jwt-token}" | cut -d. -f2 | base64 -d | jq
```

**Solution:**

1. **Check Keycloak client mappers:**
   - Keycloak Admin Console
   - Clients → eshop-client → Client Scopes → Mappers
   - Ensure "realm roles" mapper exists

2. **Add realm roles mapper** (if missing):
   - Type: User Realm Role
   - Token Claim Name: `realm_access.roles`
   - Add to ID token: ON
   - Add to access token: ON
   - Add to userinfo: ON

### Issue 5: Role Assignment Fails with "User Not Found"

**Symptoms:**
- Admin approves seller
- Error log: "User not found in Keycloak with username: xyz"
- Role not assigned

**Diagnosis:**
Username mismatch between database and Keycloak

**Solution:**

1. **Check username in database:**
   ```sql
   SELECT username, keycloak_id FROM users WHERE id = 123;
   ```

2. **Check username in Keycloak:**
   - Keycloak Admin Console → Users
   - Search for user

3. **Ensure exact match** (case-sensitive)
   - If mismatch, update database or Keycloak to match

---

## Best Practices

### Security

1. ✅ **Always use roles in endpoint security**
   ```java
   @PreAuthorize("hasRole('CUSTOMER')")
   ```

2. ✅ **Validate JWT on every request**
   - Spring Security handles this automatically

3. ✅ **Don't trust client-side role checks**
   - Always enforce on backend

4. ✅ **Use service accounts for Keycloak operations**
   - Don't use admin credentials in application

### Development

1. ✅ **Test role assignment in staging first**
2. ✅ **Monitor logs for role assignment failures**
3. ✅ **Keep Keycloak and backend in sync**
4. ✅ **Document any custom role logic**

### Production

1. ✅ **Back up Keycloak realm configuration**
2. ✅ **Monitor failed login attempts**
3. ✅ **Regular security audits**
4. ✅ **Keep Keycloak updated**

---

## Related Documentation

- [Keycloak Role Configuration](../KEYCLOAK_ROLE_CONFIGURATION.md)
- [Implementation Changes](../CHANGES_CUSTOMER_ROLE_FIX.md)
- [API Documentation](../../README.md)

---

## Appendix

### Role Matrix

| Endpoint | CUSTOMER | SELLER | DELIVERY_AGENT | ADMIN |
|----------|----------|--------|----------------|-------|
| GET /api/v1/products | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/cart | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/orders | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/sellers/register | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/products | ❌ | ✅ | ❌ | ✅ |
| GET /api/v1/dashboard/seller | ❌ | ✅ | ❌ | ✅ |
| POST /api/v1/delivery/register | ✅ | ✅ | ✅ | ✅ |
| PUT /api/v1/delivery/status | ❌ | ❌ | ✅ | ✅ |
| GET /api/v1/admin/approvals | ❌ | ❌ | ❌ | ✅ |
| POST /api/v1/admin/users | ❌ | ❌ | ❌ | ✅ |

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-17  
**Author:** Development Team
