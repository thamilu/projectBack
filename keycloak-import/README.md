# Keycloak Configuration Guide

## 📋 Overview

This directory contains the Keycloak realm configuration for the E-Shop marketplace. The configuration is production-ready and includes comprehensive security features, social login, and microservices integration.

```
┌──────────────────────────────────────────────────────────────┐
│                    ESHOP REALM ARCHITECTURE                   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ROLES (3)                                                   │
│  ├── CUSTOMER (default for all registrations)                │
│  ├── SELLER                                                  │
│  └── DELIVERY_AGENT                                          │
│                                                              │
│  CLIENTS (6)                                                 │
│  ├── eshop-client ──────── Public SPA (PKCE + S256)          │
│  ├── eshop-mobile ──────── Public Mobile App (PKCE + S256)   │
│  ├── eshop-backend ─────── Confidential Service Account      │
│  ├── eshop-order-service ─ Bearer-Only (token validation)    │
│  ├── eshop-payment-service Bearer-Only (token validation)    │
│  └── eshop-delivery-service Bearer-Only (token validation)   │
│                                                              │
│  CLIENT SCOPES (5)                                           │
│  ├── eshop-user-profile ── Phone, Address claims             │
│  ├── eshop-orders ──────── Order service audience             │
│  ├── eshop-products ────── Product catalog access            │
│  ├── eshop-payments ────── Payment service audience          │
│  └── eshop-delivery ────── Delivery service audience         │
│                                                              │
│  SOCIAL LOGIN                                                │
│  ├── Google (auto-assign CUSTOMER role)                      │
│  └── Facebook (auto-assign CUSTOMER role)                    │
│                                                              │
│  SECURITY                                                    │
│  ├── Brute Force Protection (5 failures → lockout)           │
│  ├── Password Policy (10+ chars, mixed, history)             │
│  ├── PKCE S256 (all public clients)                          │
│  ├── SSL Required (external)                                 │
│  ├── Token Revocation enabled                                │
│  ├── Access Token: 5 min                                     │
│  ├── SSO Session: 30 min idle / 10 hr max                    │
│  ├── Remember Me: 7 days idle / 30 days max                  │
│  └── OTP Support (TOTP 6-digit, 30s)                         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔑 Environment Variables

### Required Variables

All environment variables use the `${VARIABLE_NAME}` syntax in the realm configuration and must be set before importing.

#### Backend Client Secret

```bash
# Generate a strong secret (minimum 32 characters)
ESHOP_BACKEND_CLIENT_SECRET=your-strong-secret-here

# Generate using OpenSSL:
openssl rand -base64 32
```

#### SMTP Configuration

Used by Keycloak for sending emails (verification, password reset, etc.):

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_FROM_EMAIL=noreply@eshop.com
SMTP_REPLY_TO=support@eshop.com
SMTP_USER=your_email@example.com
SMTP_PASSWORD=your_email_app_password
```

> **Note**: For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833) instead of your regular password.

#### Social Login Credentials

**Google OAuth 2.0:**

```bash
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
```

Setup guide: https://console.cloud.google.com/apis/credentials

**Facebook OAuth 2.0:**

```bash
FACEBOOK_CLIENT_ID=your-facebook-app-id
FACEBOOK_CLIENT_SECRET=your-facebook-app-secret
```

Setup guide: https://developers.facebook.com/apps/

---

## 🔐 Client Configurations

### 1. eshop-client (Frontend SPA)

**Type**: Public Client  
**Flow**: Authorization Code with PKCE (S256)  
**Purpose**: Customer-facing web application

**Configuration:**
- ✅ PKCE required (S256 challenge method)
- ✅ Standard flow enabled
- ✅ Implicit flow disabled (security best practice)
- ✅ Direct access grants disabled
- ✅ Front-channel logout enabled

**Redirect URIs:**
- Development: `http://localhost:3000/*`, `http://localhost:4200/*`, `http://localhost:5173/*`, `http://localhost:8082/*`
- Production: `https://shop.yourdomain.com/*`

**Default Scopes:**
- `openid`, `profile`, `email`, `eshop-user-profile`

**Optional Scopes:**
- `offline_access`, `eshop-orders`, `eshop-products`, `eshop-payments`

**Token Claims:**
- `roles` - User's realm roles
- `user_id` - Keycloak user ID
- `email`, `email_verified` - Email information
- `phone_number` - User's phone number
- `store_id` - Seller's store ID (if applicable)

---

### 2. eshop-mobile (Mobile App)

**Type**: Public Client  
**Flow**: Authorization Code with PKCE (S256)  
**Purpose**: Native mobile applications (Android/iOS)

**Configuration:**
- ✅ PKCE required (S256 challenge method)
- ✅ Custom URI scheme: `com.eshop.mobile://`

**Redirect URIs:**
- `com.eshop.mobile://callback`
- `com.eshop.mobile://logout/callback`

---

### 3. eshop-backend (Service Account)

**Type**: Confidential Client  
**Flow**: Client Credentials  
**Purpose**: Backend service-to-Keycloak communication

**Configuration:**
- ✅ Service account enabled
- ✅ Client secret authentication
- ✅ Realm management permissions

**Service Account Roles:**
- `view-users`, `manage-users`, `query-users`, `view-realm`

**Use Cases:**
- User management operations
- Role assignment
- Token validation
- Admin operations

---

### 4-6. Microservice Clients (Bearer-Only)

**Clients:**
- `eshop-order-service`
- `eshop-payment-service`
- `eshop-delivery-service`

**Type**: Bearer-Only  
**Purpose**: Token validation for microservices

**Configuration:**
- ✅ Bearer-only mode (no login flows)
- ✅ Used for validating access tokens
- ✅ No client secrets required

---

## 🎯 Client Scopes

### eshop-user-profile

**Claims:**
- `phone_number` - User's phone number
- `shipping_address` - Default shipping address

**Consent**: Required  
**Included in**: ID token, Access token, UserInfo

---

### eshop-orders

**Purpose**: Access to order management APIs  
**Audience**: `eshop-order-service`

**Consent**: Required  
**Consent Text**: "Manage your orders"

---

### eshop-products

**Purpose**: Access to product catalog APIs

**Consent**: Required  
**Consent Text**: "Access product catalog"

---

### eshop-payments

**Purpose**: Access to payment processing APIs  
**Audience**: `eshop-payment-service`

**Consent**: Required  
**Consent Text**: "Process payments"

---

### eshop-delivery

**Purpose**: Access to delivery tracking APIs  
**Audience**: `eshop-delivery-service`

**Consent**: Required  
**Consent Text**: "Track deliveries"

---

## 👥 Roles & Permissions

### CUSTOMER (Default Role)

**Description**: Customer who browses products, places orders, and tracks deliveries

**Assigned to:**
- All new registrations
- Google OAuth users
- Facebook OAuth users

**Permissions:**
- Browse product catalog
- Place orders
- Track deliveries
- Manage profile

---

### SELLER

**Description**: Seller who manages store, products, inventory, and fulfills orders

**Assigned by**: Admin approval process

**Permissions:**
- Manage store profile
- Add/edit products
- Manage inventory
- Fulfill orders
- View analytics

**Custom Attributes:**
- `storeId` - Unique store identifier
- `storeName` - Store display name

---

### DELIVERY_AGENT

**Description**: Delivery agent who picks up and delivers orders to customers

**Assigned by**: Admin approval process

**Permissions:**
- View assigned deliveries
- Update delivery status
- Navigate to customer locations

**Custom Attributes:**
- `deliveryZone` - Assigned delivery zone
- `vehicleType` - Vehicle type (bike, car, etc.)

---

## 🔒 Security Features

### Password Policy

```
length(10) and upperCase(1) and lowerCase(1) and digits(1) and 
specialChars(1) and notUsername and notEmail and passwordHistory(5) 
and maxLength(128)
```

**Requirements:**
- Minimum 10 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit
- At least 1 special character
- Cannot be username or email
- Cannot reuse last 5 passwords
- Maximum 128 characters

---

### Brute Force Protection

**Configuration:**
- ✅ Enabled
- ✅ Max failures: 5 attempts
- ✅ Wait time: 15 minutes (900 seconds)
- ✅ Quick login check: 1 second
- ✅ Max delta time: 12 hours

**Behavior:**
- After 5 failed login attempts, account is temporarily locked
- Lock duration increases with each subsequent failure
- Permanent lockout: Disabled (allows recovery)

---

### Token Lifespans

| Token Type | Lifespan | Notes |
|------------|----------|-------|
| Access Token | 5 minutes | Short-lived for security |
| Access Token (Implicit) | 15 minutes | Legacy flow (disabled) |
| SSO Session Idle | 30 minutes | Inactive session timeout |
| SSO Session Max | 10 hours | Maximum session duration |
| Remember Me Idle | 7 days | Extended idle timeout |
| Remember Me Max | 30 days | Extended max duration |
| Offline Session Idle | 30 days | Refresh token idle |
| Offline Session Max | 60 days | Refresh token max |
| Refresh Token | Single use | Revoked after use |

---

### PKCE (Proof Key for Code Exchange)

**Configuration:**
- ✅ Required for all public clients
- ✅ Challenge method: S256 (SHA-256)
- ✅ Prevents authorization code interception attacks

**Clients using PKCE:**
- `eshop-client` (Frontend SPA)
- `eshop-mobile` (Mobile app)

---

## 🌐 Social Login Setup

### Google OAuth 2.0

**Setup Steps:**

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create a new project or select existing
3. Navigate to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
4. Application type: "Web application"
5. Add authorized redirect URIs:
   ```
   http://localhost:8080/realms/eshop/broker/google/endpoint
   https://your-domain.com/realms/eshop/broker/google/endpoint
   ```
6. Copy Client ID and Client Secret
7. Set environment variables:
   ```bash
   GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your-client-secret
   ```

**Default Behavior:**
- ✅ Auto-assigns CUSTOMER role
- ✅ Trusts email from Google
- ✅ Imports user data (no sync)

---

### Facebook OAuth 2.0

**Setup Steps:**

1. Go to [Facebook Developers](https://developers.facebook.com/apps/)
2. Create a new app or select existing
3. Add "Facebook Login" product
4. Configure OAuth redirect URIs:
   ```
   http://localhost:8080/realms/eshop/broker/facebook/endpoint
   https://your-domain.com/realms/eshop/broker/facebook/endpoint
   ```
5. Copy App ID and App Secret
6. Set environment variables:
   ```bash
   FACEBOOK_CLIENT_ID=your-facebook-app-id
   FACEBOOK_CLIENT_SECRET=your-facebook-app-secret
   ```

**Default Behavior:**
- ✅ Auto-assigns CUSTOMER role
- ⚠️ Does NOT trust email (requires verification)
- ✅ Imports user data (no sync)

---

## 📊 Event Logging

### Enabled Events (24 types)

**Authentication Events:**
- LOGIN, LOGIN_ERROR
- LOGOUT, LOGOUT_ERROR
- REGISTER, REGISTER_ERROR
- CLIENT_LOGIN, CLIENT_LOGIN_ERROR

**Token Events:**
- CODE_TO_TOKEN, CODE_TO_TOKEN_ERROR
- REFRESH_TOKEN, REFRESH_TOKEN_ERROR
- TOKEN_EXCHANGE
- REVOKE_GRANT

**User Management:**
- UPDATE_EMAIL, UPDATE_PROFILE
- RESET_PASSWORD, RESET_PASSWORD_ERROR
- SEND_VERIFY_EMAIL, SEND_RESET_PASSWORD
- VERIFY_EMAIL, VERIFY_EMAIL_ERROR

**Security:**
- FEDERATED_IDENTITY_LINK, REMOVE_FEDERATED_IDENTITY
- UPDATE_TOTP, REMOVE_TOTP
- IMPERSONATE

**Retention:** 90 days (7,776,000 seconds)

**Admin Events:**
- ✅ Enabled
- ✅ Details included
- ✅ Tracks all admin operations

---

## 🧪 Testing Guide

### 1. Verify Realm Import

```bash
# Start Keycloak
docker-compose -f docker-compose-dev.yml up -d keycloak

# Check logs for successful import
docker logs eshop-keycloak-dev | grep "Imported realm"

# Expected output:
# Imported realm 'eshop'
```

---

### 2. Access Admin Console

1. Navigate to: http://localhost:8080
2. Login with: `admin` / `admin`
3. Select "eshop" realm from dropdown
4. Verify:
   - ✅ 6 clients exist
   - ✅ 5 client scopes configured
   - ✅ 3 realm roles present
   - ✅ 2 identity providers (Google, Facebook)

---

### 3. Test Token Claims

**Example Access Token:**

```json
{
  "sub": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "user_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "email": "john@example.com",
  "email_verified": true,
  "phone_number": "+1234567890",
  "store_id": "store-123",
  "roles": ["CUSTOMER", "SELLER"],
  "aud": ["eshop-order-service", "eshop-payment-service"],
  "scope": "openid profile email eshop-orders eshop-payments",
  "exp": 1700000000,
  "iat": 1699999700,
  "iss": "http://localhost:8080/realms/eshop"
}
```

**Decode Token:**

Use [jwt.io](https://jwt.io) to decode and verify token structure.

---

### 4. Test Social Login

**Google:**
1. Navigate to frontend login page
2. Click "Sign in with Google"
3. Verify redirect to Google OAuth
4. After authentication, verify:
   - ✅ User created in Keycloak
   - ✅ CUSTOMER role assigned
   - ✅ Email marked as verified

**Facebook:**
1. Navigate to frontend login page
2. Click "Sign in with Facebook"
3. Verify redirect to Facebook OAuth
4. After authentication, verify:
   - ✅ User created in Keycloak
   - ✅ CUSTOMER role assigned
   - ⚠️ Email verification required

---

## 🚀 Production Deployment

### Pre-Deployment Checklist

- [ ] Generate strong `ESHOP_BACKEND_CLIENT_SECRET` (min 32 chars)
- [ ] Configure production SMTP server
- [ ] Set up Google OAuth with production redirect URIs
- [ ] Set up Facebook OAuth with production redirect URIs
- [ ] Update redirect URIs in realm configuration
- [ ] Enable SSL/TLS (`sslRequired: "all"`)
- [ ] Review and adjust token lifespans if needed
- [ ] Configure external database for Keycloak
- [ ] Set up monitoring and alerting
- [ ] Test all authentication flows

### Environment Variable Substitution

Keycloak automatically substitutes `${VARIABLE_NAME}` placeholders with environment variables at runtime. Ensure all variables are set in your deployment environment.

**Docker Compose Example:**

```yaml
keycloak:
  environment:
    ESHOP_BACKEND_CLIENT_SECRET: ${ESHOP_BACKEND_CLIENT_SECRET}
    SMTP_HOST: ${SMTP_HOST}
    SMTP_PORT: ${SMTP_PORT}
    # ... other variables
```

---

## 📚 Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth 2.0 PKCE](https://oauth.net/2/pkce/)
- [OpenID Connect](https://openid.net/connect/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

## 🆘 Troubleshooting

### Realm Import Failed

**Symptom:** Keycloak logs show import errors

**Solutions:**
1. Validate JSON syntax: `python -m json.tool eshop-realm.json`
2. Check environment variables are set
3. Verify Keycloak version compatibility (tested with 26.5.2)

### Social Login Not Working

**Symptom:** Redirect fails or shows error

**Solutions:**
1. Verify redirect URIs match exactly (including protocol)
2. Check client ID and secret are correct
3. Ensure social provider app is in production mode (not development)
4. Check Keycloak logs for detailed error messages

### Email Not Sending

**Symptom:** Verification emails not received

**Solutions:**
1. Verify SMTP credentials are correct
2. Check SMTP server allows connections from Keycloak
3. Test SMTP connection manually
4. Check spam folder
5. Review Keycloak logs for SMTP errors

---

**Last Updated:** 2026-02-12  
**Keycloak Version:** 26.5.2  
**Configuration Version:** 1.0.0
