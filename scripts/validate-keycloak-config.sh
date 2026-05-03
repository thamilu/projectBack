#!/bin/bash

# ============================================
# Keycloak Configuration Validation Script
# ============================================
# This script validates the Keycloak realm configuration
# and checks if all required environment variables are set.

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0
CHECKS=0

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Keycloak Configuration Validation Script            ║${NC}"
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo ""

# ============================================
# 1. Check if JSON files exist
# ============================================
echo -e "${BLUE}[1/5] Checking configuration files...${NC}"
CHECKS=$((CHECKS + 1))

if [ ! -f "keycloak-import/eshop-realm.json" ]; then
    echo -e "${RED}✗ Error: eshop-realm.json not found${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}✓ eshop-realm.json found${NC}"
fi

# ============================================
# 2. Validate JSON syntax
# ============================================
echo ""
echo -e "${BLUE}[2/5] Validating JSON syntax...${NC}"
CHECKS=$((CHECKS + 1))

if command -v python3 &> /dev/null; then
    if python3 -m json.tool keycloak-import/eshop-realm.json > /dev/null 2>&1; then
        echo -e "${GREEN}✓ JSON syntax is valid${NC}"
    else
        echo -e "${RED}✗ Error: Invalid JSON syntax${NC}"
        ERRORS=$((ERRORS + 1))
    fi
elif command -v python &> /dev/null; then
    if python -m json.tool keycloak-import/eshop-realm.json > /dev/null 2>&1; then
        echo -e "${GREEN}✓ JSON syntax is valid${NC}"
    else
        echo -e "${RED}✗ Error: Invalid JSON syntax${NC}"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${YELLOW}⚠ Warning: Python not found, skipping JSON validation${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

# ============================================
# 3. Check required environment variables
# ============================================
echo ""
echo -e "${BLUE}[3/5] Checking environment variables...${NC}"
CHECKS=$((CHECKS + 1))

# Load .env file if it exists
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo -e "${GREEN}✓ Loaded .env file${NC}"
else
    echo -e "${YELLOW}⚠ Warning: .env file not found (using .env.example as reference)${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

# Required variables
REQUIRED_VARS=(
    "ESHOP_BACKEND_CLIENT_SECRET"
    "SMTP_HOST"
    "SMTP_PORT"
    "SMTP_FROM_EMAIL"
    "SMTP_REPLY_TO"
    "SMTP_USER"
    "SMTP_PASSWORD"
    "GOOGLE_CLIENT_ID"
    "GOOGLE_CLIENT_SECRET"
    "FACEBOOK_CLIENT_ID"
    "FACEBOOK_CLIENT_SECRET"
)

echo ""
echo "Checking required environment variables:"
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo -e "${YELLOW}  ⚠ $var is not set${NC}"
        WARNINGS=$((WARNINGS + 1))
    else
        # Check if it's a placeholder value
        if [[ "${!var}" == *"your-"* ]] || [[ "${!var}" == *"example"* ]]; then
            echo -e "${YELLOW}  ⚠ $var appears to be a placeholder value${NC}"
            WARNINGS=$((WARNINGS + 1))
        else
            echo -e "${GREEN}  ✓ $var is set${NC}"
        fi
    fi
done

# ============================================
# 4. Check Keycloak connectivity (optional)
# ============================================
echo ""
echo -e "${BLUE}[4/5] Checking Keycloak connectivity...${NC}"
CHECKS=$((CHECKS + 1))

KEYCLOAK_URL="http://localhost:8080"

if command -v curl &> /dev/null; then
    if curl -s -f -o /dev/null "$KEYCLOAK_URL/health/ready"; then
        echo -e "${GREEN}✓ Keycloak is running and ready${NC}"
        
        # Check if realm exists
        if curl -s -f -o /dev/null "$KEYCLOAK_URL/realms/eshop"; then
            echo -e "${GREEN}✓ eshop realm is accessible${NC}"
        else
            echo -e "${YELLOW}⚠ Warning: eshop realm not found (may not be imported yet)${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        echo -e "${YELLOW}⚠ Warning: Keycloak is not running or not ready${NC}"
        echo -e "  Start with: docker-compose -f docker-compose-dev.yml up -d keycloak"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠ Warning: curl not found, skipping connectivity check${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

# ============================================
# 5. Verify realm configuration structure
# ============================================
echo ""
echo -e "${BLUE}[5/5] Verifying realm configuration structure...${NC}"
CHECKS=$((CHECKS + 1))

if command -v jq &> /dev/null; then
    REALM_FILE="keycloak-import/eshop-realm.json"
    
    # Check clients count
    CLIENTS_COUNT=$(jq '.clients | length' "$REALM_FILE")
    if [ "$CLIENTS_COUNT" -eq 6 ]; then
        echo -e "${GREEN}✓ Found 6 clients${NC}"
    else
        echo -e "${RED}✗ Error: Expected 6 clients, found $CLIENTS_COUNT${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    # Check client scopes count
    SCOPES_COUNT=$(jq '.clientScopes | length' "$REALM_FILE")
    if [ "$SCOPES_COUNT" -eq 5 ]; then
        echo -e "${GREEN}✓ Found 5 client scopes${NC}"
    else
        echo -e "${RED}✗ Error: Expected 5 client scopes, found $SCOPES_COUNT${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    # Check roles count
    ROLES_COUNT=$(jq '.roles.realm | length' "$REALM_FILE")
    if [ "$ROLES_COUNT" -eq 3 ]; then
        echo -e "${GREEN}✓ Found 3 realm roles${NC}"
    else
        echo -e "${RED}✗ Error: Expected 3 realm roles, found $ROLES_COUNT${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    # Check identity providers count
    IDP_COUNT=$(jq '.identityProviders | length' "$REALM_FILE")
    if [ "$IDP_COUNT" -eq 2 ]; then
        echo -e "${GREEN}✓ Found 2 identity providers${NC}"
    else
        echo -e "${RED}✗ Error: Expected 2 identity providers, found $IDP_COUNT${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
else
    echo -e "${YELLOW}⚠ Warning: jq not found, skipping structure validation${NC}"
    echo -e "  Install with: sudo apt-get install jq (Linux) or brew install jq (macOS)"
    WARNINGS=$((WARNINGS + 1))
fi

# ============================================
# Summary
# ============================================
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    Validation Summary                      ║${NC}"
echo -e "${BLUE}╠════════════════════════════════════════════════════════════╣${NC}"
echo -e "${BLUE}║${NC} Total Checks: $CHECKS"
echo -e "${BLUE}║${NC} Errors:       ${RED}$ERRORS${NC}"
echo -e "${BLUE}║${NC} Warnings:     ${YELLOW}$WARNINGS${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Configuration is ready.${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ Configuration has warnings but should work.${NC}"
    echo -e "${YELLOW}  Please review the warnings above.${NC}"
    exit 0
else
    echo -e "${RED}✗ Configuration has errors that must be fixed.${NC}"
    echo -e "${RED}  Please review the errors above.${NC}"
    exit 1
fi
