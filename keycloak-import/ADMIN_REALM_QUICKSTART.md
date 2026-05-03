# Admin Realm Quick Reference

## 🚀 Quick Setup

### 1. Set Environment Variables

Edit `.env` file:

```bash
# Admin credentials
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_INITIAL_PASSWORD=ChangeMe123!@#TempPassword

# Admin backend secret
ESHOP_ADMIN_BACKEND_SECRET=<generate-with-openssl-rand-base64-32>

# SMTP (shared with eshop realm)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_FROM_EMAIL=noreply@eshop.com
SMTP_REPLY_TO=admin@eshop.com
SMTP_USER=your_email@example.com
SMTP_PASSWORD=your_email_app_password
```

### 2. Start Keycloak

```bash
docker-compose -f docker-compose-dev.yml up -d keycloak
```

### 3. First Admin Login

1. Navigate to: http://localhost:3001
2. Login: `admin` / `${ADMIN_INITIAL_PASSWORD}`
3. **Change password** (12+ chars, 2 special chars)
4. **Setup MFA** (scan QR code with Google Authenticator)
5. Access admin dashboard

---

## 🔐 Security Features

| Feature | Configuration |
|---------|---------------|
| **Brute Force** | 3 attempts → **permanent lockout** |
| **Password Policy** | 12+ chars, 2 special, history(10), 30-day expiry |
| **MFA** | **Mandatory** TOTP for all admins |
| **Access Token** | 3 minutes (vs 5 min for eshop) |
| **SSO Session** | 15 min idle / 4 hr max |
| **Audit Logging** | 180-day retention |

---

## 🚪 Admin Login Flow

```
First Login:
1. Username/Password (temporary)
   ↓
2. FORCED: Change Password
   ↓
3. FORCED: Setup MFA/TOTP
   ↓
4. Access Dashboard

Subsequent Logins:
1. Username/Password
   ↓
2. 6-digit OTP Code
   ↓
3. Access Dashboard
```

---

## 🌐 Cross-Realm Management

**Recommended Approach:** Dual Client

```
Admin Backend uses:
├── eshop-admin-backend → Authenticate admins
└── eshop-backend → Manage eshop users
```

**Example: Approve Seller**

```java
// 1. Validate admin (eshop-admin realm)
validateAdminAccess(adminToken);

// 2. Add SELLER role (eshop realm via eshop-backend)
String serviceToken = getServiceAccountToken("eshop-backend");
addRealmRole("eshop", sellerId, "SELLER", serviceToken);
```

---

## 🧪 Testing

### Verify Realm Import

```bash
docker logs eshop-keycloak-dev | grep "Imported realm"
# Expected: Imported realm 'eshop-admin'
```

### Test Admin Login

1. Go to http://localhost:3001
2. Login with temporary password
3. Verify forced password change
4. Verify forced MFA setup
5. Login again with new password + OTP

### Test Brute Force

1. Fail login 3 times
2. Verify account locked
3. Unlock in Keycloak admin console

---

## 🆘 Common Issues

### Account Locked

**Fix:** Keycloak Admin Console → eshop-admin realm → Users → admin → Disable "Temporarily Disabled"

### MFA Code Invalid

**Fix:** Check server/phone time sync, regenerate QR code

### Email Not Sending

**Fix:** Verify SMTP credentials, use Gmail App Password

---

## 📚 Full Documentation

- **Complete Guide**: [ADMIN_REALM_README.md](./ADMIN_REALM_README.md)
- **E-Shop Realm**: [README.md](./README.md)
- **Quick Start**: [QUICKSTART.md](./QUICKSTART.md)

---

**Security Level:** MAXIMUM  
**MFA:** MANDATORY  
**Brute Force:** PERMANENT LOCKOUT
