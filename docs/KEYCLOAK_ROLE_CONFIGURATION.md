# Keycloak Role Configuration Guide

## Overview
This document describes how customer and seller roles are managed in the e-commerce application using Keycloak.

## Role Assignment Strategy

### CUSTOMER Role (Auto-Assigned)
- **When**: Automatically assigned during user registration in Keycloak
- **How**: Keycloak realm configured with `defaultRoles: ["Customer"]`
- **Approval**: No admin approval required
- **Purpose**: Allows users to browse products, add to cart, place orders

### SELLER Role (Admin Approval Required)
- **When**: Assigned after admin approves seller application
- **How**: 
  1. Customer creates seller profile via `/api/v1/sellers/register`
  2. Profile status set to `PENDING`
  3. Admin reviews via `/api/v1/admin/approvals/sellers`
  4. Admin approves → `SellerService.approveSeller()` → Assigns SELLER role in Keycloak
- **Approval**: Admin approval required
- **Purpose**: Allows users to manage products, shops, and orders

### DELIVERY_AGENT Role (Admin Approval Required)
- **When**: Assigned after admin approves delivery agent application
- **How**: Similar to SELLER role approval process
- **Approval**: Admin approval required
- **Purpose**: Allows users to manage deliveries

## Keycloak Configuration

### Option 1: Automatic Configuration (Recommended)
Import the realm configuration file that includes default role settings:

```bash
# Import realm with default roles
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json \
  --override true
```

The `realm-export.json` includes:
```json
{
  "realm": "eshop",
  "enabled": true,
  "registrationAllowed": true,
  "defaultRoles": ["Customer"],
  ...
}
```

### Option 2: Manual Configuration via Admin Console

1. **Login to Keycloak Admin Console**
   - URL: `http://localhost:8080`
   - Username: `admin`
   - Password: `admin` (or your configured password)

2. **Navigate to Realm Settings**
   - Select realm: `eshop`
   - Go to **Realm Settings** → **User Registration**

3. **Configure Default Roles**
   - Go to **Realm Roles** tab
   - Verify roles exist:
     - `Customer` (or `CUSTOMER`)
     - `Seller` (or `SELLER`)
     - `DELIVERY_AGENT`

4. **Set Default Role for New Users**
   - Go to **Realm Settings** → **User Registration** → **Default Roles**
   - Add `Customer` to the default roles list
   - Click **Save**

5. **Enable Self-Registration** (if needed)
   - Go to **Realm Settings** → **Login**
   - Enable **User registration**
   - Click **Save**

## Role Naming Convention

The application uses the following role names:
- **Keycloak Role**: `Customer` (or `CUSTOMER`)
- **Backend Constant**: `Roles.CUSTOMER = "CUSTOMER"`
- **JWT Claim**: `realm_access.roles = ["CUSTOMER"]`

**Note**: Keycloak roles are case-sensitive. Ensure consistency between:
- Keycloak role definition
- `realm-export.json` defaultRoles
- Backend `Roles.java` constants

## Testing Role Assignment

### Test CUSTOMER Role Auto-Assignment

1. **Register a new user in Keycloak**
   - URL: `http://localhost:8080/realms/eshop/account`
   - Click "Register"
   - Fill in details and submit

2. **Verify role assignment**
   - Login to Keycloak Admin Console
   - Go to **Users** → Find the new user
   - Go to **Role Mappings** tab
   - Verify `Customer` role is assigned

3. **Test in application**
   - Login via your frontend
   - Call `/api/v1/me` endpoint
   - Verify response includes `"roles": ["CUSTOMER"]`

### Test SELLER Role Admin Approval

1. **Login as customer**
   - Use credentials with CUSTOMER role

2. **Create seller profile**
   ```bash
   POST /api/v1/sellers/register
   {
     "displayName": "Test Shop",
     "email": "test@shop.com",
     "phone": "+1234567890",
     "acceptedTerms": true,
     ...
   }
   ```

3. **Verify PENDING status**
   - Profile status should be `PENDING`
   - User should NOT have SELLER role yet

4. **Admin approves**
   ```bash
   POST /api/v1/admin/approvals/sellers/{id}/APPROVE
   Headers: Authorization: Bearer {admin-token}
   ```

5. **Verify SELLER role assigned**
   - Check Keycloak Admin Console → User → Role Mappings
   - User should now have both `Customer` and `Seller` roles
   - Profile status should be `ACTIVE`

## Troubleshooting

### Problem: New users don't get CUSTOMER role

**Solution 1**: Check Keycloak default roles configuration
```bash
# Check realm configuration
curl http://localhost:8080/admin/realms/eshop | jq '.defaultRoles'
# Should return: ["Customer"]
```

**Solution 2**: Verify role exists and is spelled correctly
- Keycloak Admin Console → Realm Roles
- Ensure role name matches exactly (case-sensitive)

**Solution 3**: Backend safety net
- The `SellerService.resolveUserId()` method includes a fallback
- On first login, if CUSTOMER role is missing, it will be assigned automatically

### Problem: SELLER role not assigned after approval

**Check**:
1. Admin approval endpoint was called successfully
2. User's Keycloak ID is set in the database (`user.keycloak_id`)
3. `KeycloakService` is properly configured with admin credentials
4. Check logs for errors during role assignment

**Fix**:
```bash
# Manually assign role via Keycloak Admin API
curl -X POST http://localhost:8080/admin/realms/eshop/users/{userId}/role-mappings/realm \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '[{"name": "Seller"}]'
```

## Security Considerations

1. **Role-Based Access Control**: All endpoints use `@PreAuthorize` annotations
2. **Admin Approval**: SELLER and DELIVERY_AGENT roles require manual admin approval
3. **Customer Auto-Assignment**: Safe because CUSTOMER role has limited permissions
4. **Service Account**: Backend uses Keycloak service account for role management

## Related Files

- `realm-export.json` - Keycloak realm configuration
- `SellerService.java` - Seller registration and approval logic
- `AdminApprovalController.java` - Admin endpoints for approvals
- `KeycloakService.java` - Role assignment helper
- `Roles.java` - Role constants
- `OAuth2SecurityConfig.java` - Security configuration

## Support

For issues or questions:
1. Check Keycloak logs: `docker logs keycloak`
2. Check application logs: `logs/eshop-dev.log`
3. Verify Keycloak configuration matches `realm-export.json`
