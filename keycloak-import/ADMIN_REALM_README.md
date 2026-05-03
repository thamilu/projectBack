# E-Shop Admin Realm Documentation

## 🔐 Overview

The **eshop-admin** realm is a hardened, security-focused realm dedicated to platform administration. It enforces **mandatory MFA**, comprehensive audit logging, and strict security policies to protect administrative access.

```
┌──────────────────────────────────────────────────────────────┐
│                 ESHOP-ADMIN REALM ARCHITECTURE                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  SECURITY LEVEL: MAXIMUM                                     │
│                                                              │
│  ROLES (1)                                                   │
│  └── ADMIN - Full platform administrator                     │
│                                                              │
│  CLIENTS (2)                                                 │
│  ├── eshop-admin-frontend ─── Public SPA (PKCE + S256)       │
│  └── eshop-admin-backend ──── Confidential Service Account   │
│                                                              │
│  SECURITY FEATURES                                           │
│  ├── Brute Force: 3 attempts → PERMANENT lockout            │
│  ├── Password: 12+ chars, 2 special, history(10), 30d expiry│
│  ├── MFA: MANDATORY (TOTP required for all admins)          │
│  ├── Access Token: 3 minutes (shorter than eshop)           │
│  ├── SSO Session: 15 min idle / 4 hr max                    │
│  ├── Remember Me: 1 day idle / 3 days max                   │
│  ├── Audit Logging: 180-day retention                       │
│  └── Email Verification: Required                           │
│                                                              │
│  ADMIN USER                                                  │
│  ├── Username: admin                                         │
│  ├── Email: ${ADMIN_EMAIL}                                   │
│  ├── Password: ${ADMIN_INITIAL_PASSWORD} (temporary)         │
│  └── Required Actions: UPDATE_PASSWORD, CONFIGURE_TOTP       │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔑 Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `ADMIN_EMAIL` | Admin user email address | `admin@yourdomain.com` |
| `ADMIN_INITIAL_PASSWORD` | Temporary admin password | `ChangeMe123!@#TempPassword` |
| `ESHOP_ADMIN_BACKEND_SECRET` | Admin backend client secret | Generated 32+ char string |
| `SMTP_HOST` | Email server hostname | `smtp.gmail.com` |
| `SMTP_PORT` | Email server port | `587` |
| `SMTP_FROM_EMAIL` | Sender email address | `noreply@eshop.com` |
| `SMTP_REPLY_TO` | Reply-to email address | `admin@eshop.com` |
| `SMTP_USER` | SMTP username | `your_email@example.com` |
| `SMTP_PASSWORD` | SMTP password | App password for Gmail |

### Generate Secrets

```bash
# Admin backend client secret
openssl rand -base64 32

# Windows (PowerShell)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

---

## 🚪 Admin Login Flow

### First Login (Mandatory Steps)

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Step 1: Login with Temporary Password                     │
│  ────────────────────────────────────────────────────       │
│  Username: admin                                            │
│  Password: ${ADMIN_INITIAL_PASSWORD}                        │
│           ↓                                                 │
│                                                             │
│  Step 2: FORCED - Change Password                          │
│  ────────────────────────────────────────────────────       │
│  Requirements:                                              │
│  • Minimum 12 characters                                    │
│  • At least 1 uppercase letter                              │
│  • At least 1 lowercase letter                              │
│  • At least 1 digit                                         │
│  • At least 2 special characters                            │
│  • Cannot be username or email                              │
│  • Cannot reuse last 10 passwords                           │
│           ↓                                                 │
│                                                             │
│  Step 3: FORCED - Setup MFA/TOTP                           │
│  ────────────────────────────────────────────────────       │
│  1. Scan QR code with authenticator app:                   │
│     • Google Authenticator                                  │
│     • Microsoft Authenticator                               │
│     • FreeOTP                                               │
│  2. Enter 6-digit code to verify                            │
│           ↓                                                 │
│                                                             │
│  Step 4: Access Admin Dashboard                            │
│  ────────────────────────────────────────────────────       │
│  ✅ Admin is now logged in                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Subsequent Logins

```
1. Enter username and password
2. Enter 6-digit OTP code from authenticator app
3. Access admin dashboard
```

---

## 🔒 Security Features

### Brute Force Protection

**Configuration:**
- ✅ **Enabled** with permanent lockout
- ✅ Max failures: **3 attempts**
- ✅ Wait time: 15 minutes (900 seconds)
- ✅ Wait increment: 5 minutes (300 seconds)
- ✅ **Permanent lockout**: Enabled (admin must manually unlock)

**Behavior:**
- After 3 failed login attempts, account is **permanently locked**
- Only a Keycloak administrator can unlock the account
- This is stricter than the eshop realm (which allows recovery)

---

### Password Policy

```
length(12) and upperCase(1) and lowerCase(1) and digits(1) and 
specialChars(2) and notUsername and notEmail and passwordHistory(10) 
and maxLength(128) and forceExpiredPasswordChange(30)
```

**Requirements:**
- Minimum **12 characters** (vs 10 for eshop)
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit
- At least **2 special characters** (vs 1 for eshop)
- Cannot be username or email
- Cannot reuse last **10 passwords** (vs 5 for eshop)
- Maximum 128 characters
- **Password expires after 30 days** (forced change)

---

### Token Lifespans (Shorter for Admin Security)

| Token Type | Admin Realm | E-Shop Realm | Difference |
|------------|-------------|--------------|------------|
| Access Token | **3 minutes** | 5 minutes | 40% shorter |
| SSO Session Idle | **15 minutes** | 30 minutes | 50% shorter |
| SSO Session Max | **4 hours** | 10 hours | 60% shorter |
| Remember Me Idle | **1 day** | 7 days | 86% shorter |
| Remember Me Max | **3 days** | 30 days | 90% shorter |

**Rationale:** Admin sessions are more sensitive and require stricter timeouts.

---

### Mandatory MFA/OTP

**Configuration:**
- ✅ TOTP required for **all admin users**
- ✅ `CONFIGURE_TOTP` is a **default action** (cannot be skipped)
- ✅ Algorithm: HmacSHA256
- ✅ Digits: 6
- ✅ Period: 30 seconds

**Supported Authenticator Apps:**
- Google Authenticator
- Microsoft Authenticator
- FreeOTP

**Setup Process:**
1. Admin logs in with username/password
2. Keycloak displays QR code
3. Admin scans QR code with authenticator app
4. Admin enters 6-digit code to verify
5. MFA is now active for all future logins

---

## 📊 Event Logging & Audit Trail

### Enabled Events (23 types)

**Authentication:**
- LOGIN, LOGIN_ERROR
- LOGOUT, LOGOUT_ERROR
- CODE_TO_TOKEN, CODE_TO_TOKEN_ERROR
- CLIENT_LOGIN, CLIENT_LOGIN_ERROR

**User Management:**
- UPDATE_EMAIL, UPDATE_PROFILE, UPDATE_PASSWORD
- RESET_PASSWORD, RESET_PASSWORD_ERROR
- SEND_RESET_PASSWORD, SEND_VERIFY_EMAIL
- VERIFY_EMAIL, VERIFY_EMAIL_ERROR

**Security:**
- UPDATE_TOTP, REMOVE_TOTP
- REFRESH_TOKEN, REFRESH_TOKEN_ERROR
- IMPERSONATE
- CUSTOM_REQUIRED_ACTION

**Retention:** **180 days** (vs 90 days for eshop)

**Admin Events:**
- ✅ Enabled
- ✅ **Details included** (full audit trail)
- ✅ Tracks all admin operations (user creation, role assignment, etc.)

---

## 🔐 Client Configurations

### 1. eshop-admin-frontend (Public SPA)

**Type**: Public Client  
**Flow**: Authorization Code with PKCE (S256)  
**Purpose**: Admin dashboard web application

**Configuration:**
- ✅ PKCE required (S256 challenge method)
- ✅ Standard flow enabled
- ✅ Implicit flow disabled
- ✅ Direct access grants disabled
- ✅ Front-channel logout enabled

**Redirect URIs:**
- Development: `http://localhost:3001/*`
- Production: `https://admin.yourdomain.com/*`

**Protocol Mappers:**
- `realm-roles-mapper` - Includes ADMIN role in JWT
- `user-id-mapper` - Includes user ID in JWT
- `full-name-mapper` - Includes full name in JWT
- `email-mapper` - Includes email in JWT

---

### 2. eshop-admin-backend (Service Account)

**Type**: Confidential Client  
**Flow**: Client Credentials  
**Purpose**: Admin backend service-to-Keycloak communication

**Configuration:**
- ✅ Service account enabled
- ✅ Client secret: `${ESHOP_ADMIN_BACKEND_SECRET}`
- ✅ Realm management permissions

**Service Account Roles:**
- `view-users`, `manage-users`, `query-users`
- `view-realm`, `query-realms`
- `view-events`, `view-clients`, `query-groups`

**Use Cases:**
- Validate admin user authentication
- Manage admin realm users
- Query admin events
- Cross-realm user management (see below)

---

## 🌐 Cross-Realm Management

The admin backend needs to manage users in the **eshop** realm (customers, sellers, delivery agents). There are two approaches:

### Option A: Master Realm Service Account ❌ Not Recommended

Create a client in the `master` realm with cross-realm permissions.

**Pros:**
- Single client for all realms

**Cons:**
- ⚠️ **Security risk** - Master realm access is too powerful
- ⚠️ Violates principle of least privilege

---

### Option B: Dual Client Approach ✅ Recommended

The admin backend uses **both** clients:

1. **`eshop-admin-backend`** (in eshop-admin realm)
   - Authenticates admin users
   - Validates admin has `ADMIN` role

2. **`eshop-backend`** (in eshop realm)
   - Service account with user management permissions
   - Used to CRUD eshop realm users

**Architecture:**

```
┌──────────────────────┐
│  Admin Frontend      │
│  (localhost:3001)    │
└──────────┬───────────┘
           │ Auth Token (eshop-admin realm)
           ↓
┌──────────────────────────────────────────┐
│  Admin Backend                           │
│                                          │
│  1. Validate Admin Token                 │
│     Client: eshop-admin-backend          │
│     Realm: eshop-admin                   │
│     Check: User has ADMIN role           │
│                                          │
│  2. Manage E-Shop Users                  │
│     Client: eshop-backend                │
│     Realm: eshop                         │
│     Actions: Add/remove roles,           │
│              approve sellers, etc.       │
│                                          │
└──────────────────────────────────────────┘
           │
           ↓
┌──────────────────────────────────────────┐
│  Keycloak                                │
│                                          │
│  eshop-admin realm                       │
│  ├── Admin user authentication           │
│  └── ADMIN role validation               │
│                                          │
│  eshop realm                             │
│  ├── Customer user management            │
│  ├── Seller approval (add SELLER role)   │
│  └── Delivery agent management           │
│                                          │
└──────────────────────────────────────────┘
```

**Implementation Example:**

```java
@Service
public class AdminUserService {
    
    @Autowired
    private KeycloakAdminClient keycloakAdminClient;
    
    // Validate admin authentication (eshop-admin realm)
    public void validateAdminAccess(String adminToken) {
        // Decode JWT token
        DecodedJWT jwt = JWT.decode(adminToken);
        
        // Verify token is from eshop-admin realm
        String issuer = jwt.getIssuer();
        if (!issuer.contains("/realms/eshop-admin")) {
            throw new UnauthorizedException("Invalid realm");
        }
        
        // Verify user has ADMIN role
        List<String> roles = jwt.getClaim("roles").asList(String.class);
        if (!roles.contains("ADMIN")) {
            throw new ForbiddenException("Admin role required");
        }
    }
    
    // Approve seller (eshop realm)
    public void approveSellerRequest(String sellerId) {
        // Get service account token for eshop-backend client
        String serviceToken = keycloakAdminClient
            .getServiceAccountToken("eshop", "eshop-backend");
        
        // Add SELLER role to user in eshop realm
        keycloakAdminClient.addRealmRole(
            "eshop",
            sellerId,
            "SELLER",
            serviceToken
        );
        
        // Update user attributes (storeId, storeName)
        keycloakAdminClient.updateUserAttributes(
            "eshop",
            sellerId,
            Map.of(
                "storeId", generateStoreId(),
                "storeName", getStoreName(sellerId)
            ),
            serviceToken
        );
    }
}
```

---

## 🧪 Testing Guide

### 1. Verify Realm Import

```bash
# Start Keycloak
docker-compose -f docker-compose-dev.yml up -d keycloak

# Check logs
docker logs eshop-keycloak-dev | grep "Imported realm"

# Expected output:
# Imported realm 'eshop-admin'
```

---

### 2. Access Admin Console

1. Navigate to: http://localhost:8080
2. Login with master realm: `admin` / `admin`
3. Select "eshop-admin" realm from dropdown
4. Verify:
   - ✅ 2 clients exist
   - ✅ 1 realm role (ADMIN)
   - ✅ Brute force protection enabled
   - ✅ Password policy configured
   - ✅ Required actions include CONFIGURE_TOTP (default)
   - ✅ Admin user exists

---

### 3. Test Admin Login Flow

1. Navigate to admin frontend: http://localhost:3001
2. Login with: `admin` / `${ADMIN_INITIAL_PASSWORD}`
3. **Verify forced password change:**
   - Enter new password (12+ chars, 2 special chars)
   - Confirm new password
4. **Verify forced MFA setup:**
   - Scan QR code with Google Authenticator
   - Enter 6-digit code
5. **Verify email verification** (if SMTP configured)
6. Logout and login again:
   - Enter username and new password
   - Enter 6-digit OTP code
   - Verify access to admin dashboard

---

### 4. Test Brute Force Protection

1. Attempt to login with wrong password
2. Repeat 3 times
3. Verify account is **permanently locked**
4. Login to Keycloak admin console
5. Navigate to Users → admin → Details
6. Verify "Temporarily Disabled" is ON
7. Manually unlock the account

---

### 5. Test Cross-Realm Management

1. Login to admin dashboard
2. Navigate to "Seller Approvals"
3. Approve a pending seller
4. Verify in Keycloak admin console:
   - Switch to "eshop" realm
   - Navigate to Users → [seller user] → Role Mapping
   - Verify SELLER role is assigned
   - Navigate to Attributes
   - Verify storeId and storeName are set

---

## 🆘 Troubleshooting

### Admin Cannot Login - Account Locked

**Symptom:** "Account is disabled" error

**Cause:** Brute force protection triggered (3 failed attempts)

**Solution:**
1. Login to Keycloak admin console (master realm)
2. Select "eshop-admin" realm
3. Navigate to Users → admin → Details
4. Disable "Temporarily Disabled"
5. Click Save
6. Admin can now login again

---

### MFA Setup Failed

**Symptom:** "Invalid authenticator code" error

**Cause:** Time sync issue between server and authenticator app

**Solution:**
1. Ensure server time is correct: `date`
2. Ensure phone time is set to automatic
3. Try regenerating the QR code:
   - Login to Keycloak admin console
   - Navigate to Users → admin → Credentials
   - Delete OTP credential
   - Admin will be prompted to setup MFA again on next login

---

### Email Not Sending

**Symptom:** Email verification not received

**Solution:**
1. Verify SMTP credentials in `.env`
2. For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833)
3. Test SMTP connection:
   ```bash
   telnet smtp.gmail.com 587
   ```
4. Check Keycloak logs for SMTP errors:
   ```bash
   docker logs eshop-keycloak-dev | grep -i smtp
   ```

---

### Cross-Realm Management Not Working

**Symptom:** Admin backend cannot add roles in eshop realm

**Cause:** Service account lacks permissions

**Solution:**
1. Verify `eshop-backend` client has realm-management roles
2. Login to Keycloak admin console
3. Select "eshop" realm
4. Navigate to Clients → eshop-backend → Service Account Roles
5. Add missing roles:
   - `view-users`, `manage-users`, `query-users`
6. Restart admin backend

---

## 📚 Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [TOTP RFC 6238](https://tools.ietf.org/html/rfc6238)
- [OAuth 2.0 PKCE](https://oauth.net/2/pkce/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

**Last Updated:** 2026-02-12  
**Keycloak Version:** 26.5.2  
**Configuration Version:** 1.0.0
