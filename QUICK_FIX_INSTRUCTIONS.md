# 🔧 CUSTOMER Role Assignment - Quick Fix

## ✅ What Was Fixed

The `default-roles-eshop` composite role in `keycloak-import/eshop-realm.json` was missing the `CUSTOMER` role.

**Fixed:** Added `"CUSTOMER"` to the composite roles array (line 99).

---

## 🚀 Apply the Fix NOW

### Option 1: Automated Script (RECOMMENDED)

1. Open PowerShell or Command Prompt
2. Run:
   ```cmd
   cd G:\Project\eshop_back\scripts
   .\restart-keycloak.bat
   ```

### Option 2: Manual Docker Commands

```cmd
cd G:\Project\eshop_back
docker-compose -f docker-compose-dev.yml stop keycloak
docker-compose -f docker-compose-dev.yml rm -f keycloak
docker-compose -f docker-compose-dev.yml up -d keycloak
```

Wait 30 seconds for Keycloak to fully start.

---

## ✅ Verify the Fix

### Step 1: Check Keycloak Admin Console

1. Go to: http://localhost:8080
2. Login: `admin` / `Admin@@Secret123`
3. Navigate to: **Realm: eshop** → **Realm Settings** → **User Registration** → **Default Roles**
4. Verify `CUSTOMER` appears in the list

### Step 2: Test New User Registration

1. Register a new user (use Keycloak registration page or your frontend)
2. Login to Keycloak Admin Console
3. Go to: **Users** → Find the new user → **Role Mappings** tab
4. **✅ VERIFY:** `CUSTOMER` role should be automatically assigned

### Step 3: Test API Access

1. Login as the new user in your frontend
2. Call: `GET /api/v1/me`
3. **✅ VERIFY:** Response should include `"roles": ["CUSTOMER"]`

---

## 🎯 What Changed

### File Modified
`keycloak-import/eshop-realm.json` (Line 91-100)

**Before:**
```json
{
  "name": "default-roles-eshop",
  "composite": true,
  "composites": {
    "realm": [
      "offline_access",
      "uma_authorization"
    ]
  }
}
```

**After:**
```json
{
  "name": "default-roles-eshop",
  "composite": true,
  "composites": {
    "realm": [
      "offline_access",
      "uma_authorization",
      "CUSTOMER"          ← ADDED
    ]
  }
}
```

---

## 🔍 Troubleshooting

### Issue: CUSTOMER role still not assigned

**Solution:**
1. Ensure you restarted Keycloak **after** the fix
2. Check if you're using the correct realm (`eshop`)
3. Verify `keycloak-import/eshop-realm.json` contains the fix
4. Check Keycloak logs: `docker logs eshop-keycloak-dev`

### Issue: "default-roles-eshop-1" still appears

**Solution:**
This is the composite role container - it's normal to see it.
The important thing is that CUSTOMER is **inside** this composite role.

### Issue: Existing users don't have CUSTOMER role

**Solution:**
Existing users need manual role assignment or backend JIT sync:
1. **Manual:** Admin Console → Users → Select User → Role Mappings → Assign `CUSTOMER`
2. **Automatic:** They'll get the role on next login (backend JIT sync handles this)

---

## 📚 Full Documentation

- **Role Management:** `docs/guides/ROLE_MANAGEMENT.md`
- **Keycloak Setup:** `docs/setup/KEYCLOAK_SETUP.md`
- **Deployment Guide:** `docs/deployment/DEPLOYMENT_GUIDE.md`

---

## ✅ Summary

| Step | Status |
|------|--------|
| Fixed `eshop-realm.json` | ✅ Done |
| Restart Keycloak | ⏳ **Do this now** |
| Test new user registration | ⏳ After restart |
| Verify CUSTOMER role | ⏳ After test |

**Next:** Run `scripts\restart-keycloak.bat` to apply the fix!
