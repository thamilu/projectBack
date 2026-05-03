# Keycloak Quick Start Guide

## 🚀 Quick Setup (Development)

### 1. Create Environment File

```bash
# Copy the example file
cp .env.example .env
```

### 2. Generate Backend Client Secret

**Linux/macOS:**
```bash
openssl rand -base64 32
```

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

Copy the output and update `.env`:
```bash
ESHOP_BACKEND_CLIENT_SECRET=<paste-generated-secret-here>
```

### 3. Start Keycloak

```bash
docker-compose -f docker-compose-dev.yml up -d keycloak
```

### 4. Verify Import

```bash
# Check logs
docker logs eshop-keycloak-dev | grep "Imported realm"

# Expected output:
# Imported realm 'eshop'
```

### 5. Access Admin Console

- URL: http://localhost:8080
- Username: `admin`
- Password: `admin`
- Realm: Select "eshop" from dropdown

---

## 📋 Environment Variables Reference

### Required for Production

| Variable | Description | Example |
|----------|-------------|---------|
| `ESHOP_BACKEND_CLIENT_SECRET` | Backend service account secret | Generated 32+ char string |
| `SMTP_HOST` | Email server hostname | `smtp.gmail.com` |
| `SMTP_PORT` | Email server port | `587` |
| `SMTP_FROM_EMAIL` | Sender email address | `noreply@eshop.com` |
| `SMTP_REPLY_TO` | Reply-to email address | `support@eshop.com` |
| `SMTP_USER` | SMTP username | `your_email@example.com` |
| `SMTP_PASSWORD` | SMTP password | App password for Gmail |

### Optional (Social Login)

| Variable | Description | Setup Guide |
|----------|-------------|-------------|
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | [Google Console](https://console.cloud.google.com/apis/credentials) |
| `GOOGLE_CLIENT_SECRET` | Google OAuth secret | Same as above |
| `FACEBOOK_CLIENT_ID` | Facebook app ID | [Facebook Developers](https://developers.facebook.com/apps/) |
| `FACEBOOK_CLIENT_SECRET` | Facebook app secret | Same as above |

---

## 🔐 Realm Configuration Overview

### Clients

| Client ID | Type | Purpose |
|-----------|------|---------|
| `eshop-client` | Public | Frontend SPA (PKCE S256) |
| `eshop-mobile` | Public | Mobile app (PKCE S256) |
| `eshop-backend` | Confidential | Backend service account |
| `eshop-order-service` | Bearer-Only | Order microservice |
| `eshop-payment-service` | Bearer-Only | Payment microservice |
| `eshop-delivery-service` | Bearer-Only | Delivery microservice |

### Roles

- **CUSTOMER** - Default role (auto-assigned)
- **SELLER** - Manages stores and products
- **DELIVERY_AGENT** - Handles deliveries

### Token Lifespans

- Access Token: **5 minutes**
- SSO Session: **30 min idle** / **10 hr max**
- Remember Me: **7 days idle** / **30 days max**

---

## 🧪 Testing

### Validate Configuration

**Windows:**
```bash
.\scripts\validate-keycloak-config.bat
```

**Linux/macOS:**
```bash
bash scripts/validate-keycloak-config.sh
```

### Test Login Flow

1. Start frontend application
2. Navigate to login page
3. Click "Sign in with Google" or "Sign in with Facebook"
4. Verify redirect and authentication

---

## 📚 Documentation

- **Full Documentation**: [keycloak-import/README.md](./README.md)
- **Implementation Plan**: See artifacts directory
- **Walkthrough**: See artifacts directory

---

## 🆘 Common Issues

### Realm Not Imported

**Symptom:** Admin console doesn't show "eshop" realm

**Solution:**
```bash
# Restart Keycloak
docker-compose -f docker-compose-dev.yml restart keycloak

# Check logs
docker logs eshop-keycloak-dev
```

### Email Not Sending

**Symptom:** Verification emails not received

**Solution:**
1. Verify SMTP credentials in `.env`
2. For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833)
3. Check spam folder

### Social Login Fails

**Symptom:** Redirect error after OAuth

**Solution:**
1. Verify redirect URIs match exactly
2. Check client ID and secret are correct
3. Ensure app is in production mode (not development)

---

## 🎯 Next Steps

1. ✅ Configuration is complete
2. ⏭️ Set up environment variables
3. ⏭️ Configure SMTP (optional for dev)
4. ⏭️ Set up social login (optional)
5. ⏭️ Start Keycloak and verify import
6. ⏭️ Test authentication flows

---

**For detailed information, see [README.md](./README.md)**
