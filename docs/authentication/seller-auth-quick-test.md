# Seller Authentication - Quick Test Guide

## 🚀 Test in 2 Minutes

### Prerequisites
- ✅ Keycloak running on http://localhost:8080
- ✅ Backend running on http://localhost:8082
- ✅ Seller user created in Keycloak (seller@gmail.com / password123)

### Method 1: PowerShell Script (Recommended)

```powershell
# Run this command:
.\test-seller-dashboard.ps1
```

**Expected Output:**
```
========================================
Seller Dashboard Authentication Test
========================================

Step 1: Authenticating seller user...
  Email: seller@gmail.com
✅ Authentication successful!

Step 2: Decoding JWT token...
  User: seller@gmail.com
  Roles: SELLER
  Subject: xxx-xxx-xxx

Step 3: Calling seller dashboard endpoint...
  Endpoint: GET http://localhost:8082/api/v1/dashboard/seller
✅ Seller dashboard accessed successfully!

========================================
✅ ALL TESTS PASSED
========================================
```

### Method 2: HTML Test Page

```powershell
# Open in browser:
start test-seller-dashboard.html

# Then:
# 1. Click "1. Login & Get Token"
# 2. Click "2. Call Seller Dashboard"
```

### Expected Backend Logs

After running the test, check your Spring Boot logs:

```log
INFO  c.e.a.controller.DashboardController - ✅ SELLER authenticated | user=seller@gmail.com | roles=[SELLER] | sellerId=XXX
DEBUG c.e.a.controller.DashboardController - Seller dashboard requested for seller ID: XXX
URI: /api/v1/dashboard/seller
Status: 200
```

## ✅ Success Criteria

You'll know it's working when you see:
1. ✅ PowerShell script shows "ALL TESTS PASSED"
2. ✅ Backend logs show "✅ SELLER authenticated"
3. ✅ HTTP Status: 200 OK
4. ✅ Dashboard data returned in response

## 🔧 Troubleshooting

### Problem: "Authentication failed"
**Solution:** Check Keycloak is running:
```powershell
curl http://localhost:8080
```

### Problem: "Dashboard call failed"
**Solution:** Check backend is running:
```powershell
curl http://localhost:8082/actuator/health
```

### Problem: "Access denied (403)"
**Solution:** Verify seller has SELLER role in Keycloak:
1. Open http://localhost:8080/admin
2. Login as admin
3. Users → seller@gmail.com → Role Mapping
4. Ensure "SELLER" role is assigned

## 📋 What Was Fixed

1. **Enhanced logging** in DashboardController
2. **Created test scripts** for easy verification
3. **Added documentation** with examples

All security configurations were already correct! The issue was just lack of visibility into seller authentication success.

---

**Need help?** Check [SELLER_AUTHENTICATION_FIX_COMPLETE.md](./SELLER_AUTHENTICATION_FIX_COMPLETE.md) for detailed troubleshooting.
