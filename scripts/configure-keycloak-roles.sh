#!/bin/bash

# Keycloak Role Configuration Script
# This script helps configure CUSTOMER as a default role in Keycloak

set -e

echo "=========================================="
echo "Keycloak Role Configuration Script"
echo "=========================================="
echo ""

# Configuration
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${KEYCLOAK_REALM:-eshop}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

echo "Configuration:"
echo "  Keycloak URL: $KEYCLOAK_URL"
echo "  Realm: $REALM"
echo "  Admin User: $ADMIN_USER"
echo ""

# Step 1: Get admin access token
echo "[1/4] Getting admin access token..."
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo "❌ Failed to get admin token. Check credentials."
  exit 1
fi
echo "✅ Admin token obtained"
echo ""

# Step 2: Check if roles exist
echo "[2/4] Checking if roles exist..."
ROLES=("CUSTOMER" "SELLER" "DELIVERY_AGENT" "Customer" "Seller")

for ROLE in "${ROLES[@]}"; do
  ROLE_CHECK=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles/$ROLE" \
    -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.name // empty')
  
  if [ -n "$ROLE_CHECK" ]; then
    echo "  ✅ Role '$ROLE' exists"
  else
    echo "  ℹ️  Role '$ROLE' not found, creating..."
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\": \"$ROLE\", \"description\": \"$ROLE role\"}"
    echo "  ✅ Role '$ROLE' created"
  fi
done
echo ""

# Step 3: Get current realm configuration
echo "[3/4] Getting current realm configuration..."
CURRENT_CONFIG=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

CURRENT_DEFAULT_ROLES=$(echo "$CURRENT_CONFIG" | jq -r '.defaultRoles[]? // empty' | tr '\n' ',' | sed 's/,$//')
echo "  Current default roles: ${CURRENT_DEFAULT_ROLES:-none}"
echo ""

# Step 4: Set default role to Customer
echo "[4/4] Setting 'Customer' as default role..."

# Update realm to add Customer as default role
curl -s -X PUT "$KEYCLOAK_URL/admin/realms/$REALM" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "'"$REALM"'",
    "defaultRoles": ["Customer"],
    "registrationAllowed": true
  }'

echo "✅ Default role configuration updated"
echo ""

# Verify the changes
echo "=========================================="
echo "Verification"
echo "=========================================="
UPDATED_CONFIG=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

UPDATED_DEFAULT_ROLES=$(echo "$UPDATED_CONFIG" | jq -r '.defaultRoles[]? // empty' | tr '\n' ', ' | sed 's/, $//')
REGISTRATION_ALLOWED=$(echo "$UPDATED_CONFIG" | jq -r '.registrationAllowed')

echo "✅ Configuration complete!"
echo ""
echo "Summary:"
echo "  Default Roles: $UPDATED_DEFAULT_ROLES"
echo "  Registration Allowed: $REGISTRATION_ALLOWED"
echo ""
echo "Next Steps:"
echo "  1. Test by registering a new user"
echo "  2. Verify the user has 'Customer' role assigned"
echo "  3. Login and access customer endpoints"
echo ""
echo "For more details, see: docs/KEYCLOAK_ROLE_CONFIGURATION.md"
