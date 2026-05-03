# Fix Default Roles in Keycloak - Step by Step

## Problem
You're seeing `default-roles-eshop-1` instead of `Customer` role being assigned to new users.

## Solution - Manual Configuration via Keycloak Admin Console

### Step 1: Access Keycloak Admin Console

1. Open browser and go to: **http://localhost:8080**
2. Click **Administration Console**
3. Login with:
   - Username: `admin`
   - Password: `admin`

### Step 2: Select Your Realm

1. Click on the **dropdown in top-left corner** (currently showing "master")
2. Select **eshop** realm

### Step 3: Navigate to Realm Roles

1. In the left sidebar, click on **Realm roles**
2. You should see roles:
   - Customer
   - Seller
   - DELIVERY_AGENT
   - default-roles-eshop (or similar)

### Step 4: Configure Default Roles (IMPORTANT)

1. In the left sidebar, click on **Realm settings**
2. Click on the **User registration** tab
3. Scroll down to the **Default roles** section
4. You'll see a list of currently assigned default roles

### Step 5: Remove Wrong Default Role

1. Look for `default-roles-eshop-1` or `default-roles-eshop` in the default roles list
2. Click the **X** button next to it to remove it

### Step 6: Add Correct Default Role

1. Click the **Assign role** button
2. In the popup, find and select **Customer** role
3. Click **Assign**
4. Verify that **Customer** now appears in the Default roles list

### Step 7: Verify Configuration

1. Still in **Realm settings** → **User registration** tab
2. Confirm:
   - ✅ User registration is **ON** (enabled)
   - ✅ Default roles shows: **Customer**

### Step 8: Test with a New User

1. Open a new incognito/private browser window
2. Go to: **http://localhost:8080/realms/eshop/account**
3. Click **Register**
4. Fill in the form:
   - Username: `test_user_001`
   - Email: `test_user_001@test.com`
   - First name: `Test`
   - Last name: `User`
   - Password: `TestPassword123!`
   - Confirm password: `TestPassword123!`
5. Click **Register**

### Step 9: Verify Role Assignment

1. Go back to Keycloak Admin Console
2. Click on **Users** in the left sidebar
3. Search for: `test_user_001`
4. Click on the username to open user details
5. Click on **Role mappings** tab
6. Under **Assigned roles**, you should see:
   - ✅ **Customer** role

### Step 10: Test Login

1. Try logging in with the new user
2. Call the API: `GET /api/v1/me`
3. Response should include: `"roles": ["CUSTOMER"]`

---

## Alternative: Use Script to Fix

If you prefer command-line approach:

```bash
# Windows PowerShell
cd G:\Project\eshop_back\scripts
.\configure-keycloak-roles.bat

# Or use the manual steps above (recommended)
```

---

## What This Fixes

**Before:**
- Default role: `default-roles-eshop-1` (internal Keycloak role)
- New users don't get CUSTOMER role
- Users can't access customer endpoints

**After:**
- Default role: `Customer` (our application role)
- New users automatically get CUSTOMER role
- Users can immediately access customer endpoints

---

## Troubleshooting

### If you don't see "Customer" role in the list:

1. Go to **Realm roles** in left sidebar
2. Click **Create role**
3. Role name: `Customer`
4. Description: `Customer role for browsing and purchasing`
5. Click **Save**
6. Then go back to **Realm settings** → **User registration** → **Default roles**
7. Assign the newly created **Customer** role

### If configuration doesn't save:

1. Make sure you clicked **Save** button after changes
2. Check browser console for errors (F12)
3. Try logging out and back into Keycloak Admin Console
4. Restart Keycloak container: `docker-compose restart keycloak`

### If new users still don't get the role:

1. Delete the test user you created
2. Clear browser cache
3. Try registering a new user again
4. If still failing, check Keycloak logs: `docker logs keycloak`

---

## Need More Help?

See full documentation in:
- `docs/KEYCLOAK_ROLE_CONFIGURATION.md`
- `docs/setup/KEYCLOAK_SETUP.md`
