# Customer Role Auto-Assignment - Implementation Summary

## Date: 2026-02-17

## Problem Statement
- Users registering via Keycloak UI were not getting the CUSTOMER role automatically
- This prevented them from accessing customer-protected endpoints
- Seller auto-approval was enabled, bypassing the intended admin approval workflow

## Solution Implemented

### 1. Keycloak Configuration Update

**File**: `realm-export.json`

Added default role configuration:
```json
{
  "realm": "eshop",
  "registrationAllowed": true,
  "registrationEmailAsUsername": false,
  "editUsernameAllowed": false,
  "resetPasswordAllowed": true,
  "defaultRoles": ["Customer"],
  ...
}
```

**Impact**: All new users registering in Keycloak will automatically receive the `Customer` role.

### 2. Backend Safety Net

**File**: `SellerService.java`

Added CUSTOMER role assignment during JIT (Just-In-Time) user creation:

```java
// In resolveUserId() method - lines 276-287
Long userId = userService.createUserFromClaims(username, email, firstName, lastName, emailVerified, keycloakId);

// Safety net: Ensure CUSTOMER role is assigned
try {
    log.info("Assigning CUSTOMER role to new user: {}", username);
    keycloakService.assignRoleByUsername(username, com.eshop.app.constants.Roles.CUSTOMER);
} catch (Exception e) {
    log.warn("Failed to assign CUSTOMER role to {}: {}", username, e.getMessage());
}

return userId;
```

**Impact**: If Keycloak default role configuration fails, the backend will assign CUSTOMER role on first login.

### 3. Seller Auto-Approval Removed

**File**: `SellerService.java`

Removed auto-approval logic in `registerSeller()` method (lines 93-108):

**Before**:
```java
// Auto-approve (Enabled for dev/fresh setup as requested)
try {
    approveSeller(saved.getId(), "SYSTEM (Auto-Approve)");
    return toResponse(sellerProfileRepository.findById(saved.getId()).orElse(saved));
} catch (Exception e) {
    log.error("Auto-approval failed: {}", e.getMessage());
    return toResponse(saved);
}
```

**After**:
```java
log.info("Seller profile created with id: {} for userId: {}, status: PENDING (awaiting admin approval)", saved.getId(), userId);
return toResponse(saved);
```

**Impact**: Seller applications now require admin approval via `AdminApprovalController`.

## User Flows After Implementation

### Flow 1: Customer Registration
1. User registers on Keycloak (via Keycloak UI or self-registration page)
2. Keycloak automatically assigns `Customer` role (via default roles)
3. User logs into the e-commerce app
4. Backend validates JWT with `Customer` role
5. User can access customer endpoints (cart, orders, etc.)

### Flow 2: Customer → Seller Upgrade
1. Logged-in customer navigates to "Become a Seller"
2. Submits seller profile via `POST /api/v1/sellers/register`
3. Seller profile created with status: `PENDING`
4. Admin reviews application via `GET /api/v1/admin/approvals/sellers`
5. Admin approves via `POST /api/v1/admin/approvals/sellers/{id}/APPROVE`
6. `SellerService.approveSeller()` executes:
   - Changes status to `ACTIVE`
   - Assigns `SELLER` role in Keycloak via `KeycloakService.assignRoleByUsername()`
7. User now has both `Customer` and `Seller` roles
8. User can access seller endpoints (products, shop management, etc.)

### Flow 3: Delivery Agent Registration
- Similar to seller flow
- Requires admin approval
- Assigns `DELIVERY_AGENT` role after approval

## Files Modified

1. **realm-export.json** - Keycloak realm configuration
2. **SellerService.java** - Removed auto-approval, added CUSTOMER role safety net
3. **docs/KEYCLOAK_ROLE_CONFIGURATION.md** - Comprehensive documentation (new)
4. **scripts/configure-keycloak-roles.sh** - Linux/Mac configuration script (new)
5. **scripts/configure-keycloak-roles.bat** - Windows configuration script (new)

## Deployment Steps

### Step 1: Update Keycloak Configuration

**Option A: Re-import realm** (recommended for fresh setup)
```bash
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json \
  --override true
```

**Option B: Manual configuration** (for existing production)
```bash
# Run the configuration script
cd scripts
./configure-keycloak-roles.sh

# Or for Windows
configure-keycloak-roles.bat
```

**Option C: Via Keycloak Admin Console**
1. Login to Keycloak Admin Console: http://localhost:8080
2. Select realm: `eshop`
3. Go to: **Realm Settings** → **User Registration** → **Default Roles**
4. Add `Customer` to default roles
5. Save

### Step 2: Deploy Backend Code
```bash
# Rebuild the application
./gradlew clean build

# Restart the service
docker-compose restart eshop-backend
```

### Step 3: Verify Configuration
```bash
# Test 1: Register a new user via Keycloak
# Test 2: Check if Customer role is assigned
# Test 3: Login and verify JWT contains Customer role
```

## Testing Checklist

- [ ] **Customer Registration**
  - [ ] Register new user via Keycloak UI
  - [ ] Verify `Customer` role is auto-assigned (check Keycloak Admin Console)
  - [ ] Login to the app
  - [ ] Call `/api/v1/me` - verify `roles` includes `CUSTOMER`
  - [ ] Access customer endpoint (e.g., `/api/v1/cart`) - should work

- [ ] **Seller Application Flow**
  - [ ] Login as customer
  - [ ] Submit seller profile via `POST /api/v1/sellers/register`
  - [ ] Verify profile status is `PENDING`
  - [ ] Verify user does NOT have `SELLER` role yet
  - [ ] Login as admin
  - [ ] Get pending sellers via `GET /api/v1/admin/approvals/sellers`
  - [ ] Approve seller via `POST /api/v1/admin/approvals/sellers/{id}/APPROVE`
  - [ ] Verify seller profile status is now `ACTIVE`
  - [ ] Verify user now has `SELLER` role in Keycloak
  - [ ] Login as seller and access seller endpoints

- [ ] **JIT User Creation Safety Net**
  - [ ] Register user in Keycloak but manually remove `Customer` role
  - [ ] Login to the app for the first time
  - [ ] Check logs - should see "Assigning CUSTOMER role to new user"
  - [ ] Verify `Customer` role is now assigned

## Rollback Plan

If issues occur, rollback by:

1. **Revert code changes**:
```bash
git checkout HEAD~1 -- src/main/java/com/eshop/app/service/SellerService.java
git checkout HEAD~1 -- realm-export.json
```

2. **Re-enable auto-approval** (temporary):
   - Edit `SellerService.registerSeller()` line ~95
   - Uncomment the auto-approval block

3. **Manually assign Customer role** to existing users via Keycloak Admin Console

## Security Considerations

✅ **Safe Changes**:
- CUSTOMER role has limited permissions (browse, cart, orders)
- SELLER role still requires admin approval
- DELIVERY_AGENT role still requires admin approval
- No changes to authentication flow

⚠️ **Important**:
- Ensure Keycloak service account has `manage-users` and `manage-realm` permissions
- Monitor logs for role assignment failures
- Test thoroughly in staging before production deployment

## Support & Troubleshooting

See detailed troubleshooting guide in:
- `docs/KEYCLOAK_ROLE_CONFIGURATION.md`

Common issues:
1. **Role not assigned** → Check Keycloak default roles configuration
2. **Auto-approval still happening** → Verify code changes deployed
3. **Permission denied** → Check JWT roles in `/api/v1/me` response

## Related Documentation

- [Keycloak Role Configuration Guide](./KEYCLOAK_ROLE_CONFIGURATION.md)
- [Database Operations SOP](./database/DATABASE_OPERATIONS_SOP.md)

## Contributors

- Implementation Date: 2026-02-17
- Implemented by: Rovo Dev
- Reviewed by: [Pending Review]
