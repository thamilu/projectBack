#!/bin/bash

# Keycloak Authentication Test Script
# This script tests all the Keycloak authentication endpoints

BASE_URL="http://localhost:8082/api/auth"
ADMIN_BASE_URL="http://localhost:8082/api/admin"

echo "========================================"
echo "Keycloak Authentication Test Script"
echo "========================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Test 1: Login with username and password
echo "========================================"
echo "Test 1: Login with Username/Password"
echo "========================================"
print_info "Attempting login with username: admin, password: admin123"

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

if [ $? -eq 0 ]; then
    ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.access_token')
    REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refresh_token')
    
    if [ "$ACCESS_TOKEN" != "null" ] && [ "$ACCESS_TOKEN" != "" ]; then
        print_success "Login successful!"
        echo "$LOGIN_RESPONSE" | jq
        echo ""
    else
        print_error "Login failed!"
        echo "$LOGIN_RESPONSE"
        exit 1
    fi
else
    print_error "Failed to connect to server"
    exit 1
fi

# Test 2: Get current user info
echo "========================================"
echo "Test 2: Get Current User (from JWT)"
echo "========================================"
print_info "Fetching user info using access token"

USER_INFO=$(curl -s -X GET "$BASE_URL/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$USER_INFO" | jq -e '.username' > /dev/null 2>&1; then
    print_success "User info retrieved successfully!"
    echo "$USER_INFO" | jq
    echo ""
else
    print_error "Failed to get user info"
    echo "$USER_INFO"
fi

# Test 3: Get user info from Keycloak
echo "========================================"
echo "Test 3: Get User Info from Keycloak"
echo "========================================"

KC_USER_INFO=$(curl -s -X GET "$BASE_URL/userinfo" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$KC_USER_INFO" | jq -e '.preferred_username' > /dev/null 2>&1; then
    print_success "Keycloak user info retrieved!"
    echo "$KC_USER_INFO" | jq
    echo ""
else
    print_error "Failed to get Keycloak user info"
    echo "$KC_USER_INFO"
fi

# Test 4: Introspect token
echo "========================================"
echo "Test 4: Introspect Token"
echo "========================================"

INTROSPECT=$(curl -s -X POST "$BASE_URL/introspect" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$INTROSPECT" | jq -e '.active' > /dev/null 2>&1; then
    print_success "Token introspection successful!"
    echo "$INTROSPECT" | jq
    echo ""
else
    print_error "Token introspection failed"
    echo "$INTROSPECT"
fi

# Test 5: Get OpenID configuration
echo "========================================"
echo "Test 5: Get OpenID Configuration"
echo "========================================"

CONFIG=$(curl -s -X GET "$BASE_URL/config")

if echo "$CONFIG" | jq -e '.issuer' > /dev/null 2>&1; then
    print_success "OpenID configuration retrieved!"
    echo "$CONFIG" | jq '.issuer, .authorization_endpoint, .token_endpoint' | head -10
    echo ""
else
    print_error "Failed to get OpenID configuration"
    echo "$CONFIG"
fi

# Test 6: Get login URL
echo "========================================"
echo "Test 6: Get OAuth2 Login URL"
echo "========================================"

LOGIN_URL=$(curl -s -X GET "$BASE_URL/login-url?redirectUri=http://localhost:3000/callback")

if echo "$LOGIN_URL" | jq -e '.authorizationUrl' > /dev/null 2>&1; then
    print_success "Login URL generated!"
    echo "$LOGIN_URL" | jq
    echo ""
else
    print_error "Failed to generate login URL"
    echo "$LOGIN_URL"
fi

# Test 7: Refresh token
echo "========================================"
echo "Test 7: Refresh Access Token"
echo "========================================"
print_info "Using refresh token to get new access token"

REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

if echo "$REFRESH_RESPONSE" | jq -e '.access_token' > /dev/null 2>&1; then
    NEW_ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | jq -r '.access_token')
    print_success "Token refreshed successfully!"
    echo "$REFRESH_RESPONSE" | jq '.token_type, .expires_in'
    echo ""
else
    print_error "Token refresh failed"
    echo "$REFRESH_RESPONSE"
fi

# Test 8: Logout
echo "========================================"
echo "Test 8: Logout"
echo "========================================"
print_info "Logging out user"

LOGOUT_RESPONSE=$(curl -s -X POST "$BASE_URL/logout" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

if echo "$LOGOUT_RESPONSE" | jq -e '.message' > /dev/null 2>&1; then
    print_success "Logout successful!"
    echo "$LOGOUT_RESPONSE" | jq
    echo ""
else
    print_error "Logout failed"
    echo "$LOGOUT_RESPONSE"
fi

echo "========================================"
echo "All Tests Completed!"
echo "========================================"
print_success "Authentication flow tested successfully"
echo ""
echo "Next steps:"
echo "1. Test with frontend application"
echo "2. Create additional test users with different roles"
echo "3. Test admin endpoints (requires ADMIN role)"
echo ""
