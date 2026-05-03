# 🚀 Quick Start: JWT Authentication Setup

## ⚡ Files Changed

1. ✅ **`Roles.java`** - New role constants class
2. ✅ **`OAuth2SecurityConfig.java`** - Reads from `"roles"` claim
3. ✅ **`DashboardController.java`** - Updated `@PreAuthorize` to allow ADMIN
4. ✅ **`MeController.java`** - Enhanced JWT debugging endpoint

## 🔥 What Works NOW

### ✅ JWT Token Validation
- Spring automatically validates JWT from Keycloak
- No custom filters needed
- Stateless authentication

### ✅ Role Extraction
Reads roles directly from JWT `"roles"` claim:
```json
{
  "roles": ["SELLER", "ADMIN"]
}
```

### ✅ Authorization
- `@PreAuthorize("hasRole('ADMIN')")` → Admin only
- `@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")` → Seller OR Admin

## 🔴 ONE CRITICAL STEP LEFT: Keycloak Mapper

**Go to Keycloak Admin Console:**

1. **Client Scopes** → `roles` → **Mappers** → **Add mapper**
2. Select **"User Realm Role"**
3. Configure:
   - **Token Claim Name:** `roles` ⚠️
   - **Multivalued:** `ON` ✅
   - **Add to access token:** `ON` ✅

**That's it!** ✅

## 🧪 Test It

```bash
# Get token
TOKEN=$(curl -X POST "http://localhost:8080/realms/eshop/protocol/openid-connect/token" \
  -d "client_id=eshop-client" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=seller@test.com" \
  -d "password=password" \
  -d "grant_type=password" | jq -r '.access_token')

# Test /me endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/me | jq

# Test seller dashboard
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/v1/dashboard/seller | jq
```

## ✅ Expected Response from /me

```json
{
  "sub": "user-id",
  "userId": "user-id",
  "username": "seller@test.com",
  "email": "seller@test.com",
  "roles": ["SELLER"],
  "authorities": ["ROLE_SELLER"],
  "tokenIssuedAt": "2026-01-01T10:00:00Z",
  "tokenExpiresAt": "2026-01-01T11:00:00Z"
}
```

## 🎯 Endpoint Access Control

| Endpoint | ADMIN | SELLER | CUSTOMER |
|----------|-------|--------|----------|
| `/api/v1/dashboard/admin` | ✅ | ❌ | ❌ |
| `/api/v1/dashboard/seller` | ✅ | ✅ | ❌ |
| `/api/v1/dashboard/customer` | ❌ | ❌ | ✅ |
| `/api/me` | ✅ | ✅ | ✅ |

## 🔧 Debugging Tips

**No roles in JWT?**
→ Check Keycloak mapper configuration

**401 Unauthorized?**
→ Check token expiration, verify Authorization header

**403 Forbidden?**
→ User doesn't have required role, check `/api/me`

## 📝 Key Files to Review

- [JWT_AUTHENTICATION_IMPLEMENTATION.md](JWT_AUTHENTICATION_IMPLEMENTATION.md) - Full documentation
- `src/main/java/com/eshop/app/constants/Roles.java` - Role constants
- `src/main/java/com/eshop/app/config/OAuth2SecurityConfig.java` - Security config
- `src/main/java/com/eshop/app/controller/MeController.java` - Debug endpoint

---

**Status:** ✅ Implementation Complete | ⚠️ Keycloak mapper pending  
**Build:** ✅ Successful  
**Date:** January 1, 2026
