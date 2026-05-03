@echo off
REM Keycloak Role Configuration Script for Windows
REM This script helps configure CUSTOMER as a default role in Keycloak

setlocal enabledelayedexpansion

echo ==========================================
echo Keycloak Role Configuration Script
echo ==========================================
echo.

REM Configuration
set KEYCLOAK_URL=http://localhost:8080
set REALM=eshop
set ADMIN_USER=admin
set ADMIN_PASS=admin

echo Configuration:
echo   Keycloak URL: %KEYCLOAK_URL%
echo   Realm: %REALM%
echo   Admin User: %ADMIN_USER%
echo.

echo ==========================================
echo IMPORTANT: Manual Configuration Required
echo ==========================================
echo.
echo This script provides instructions for manual configuration.
echo Automated configuration requires 'curl' and 'jq' commands.
echo.
echo Manual Steps:
echo.
echo 1. Open Keycloak Admin Console
echo    URL: %KEYCLOAK_URL%
echo    Username: %ADMIN_USER%
echo    Password: %ADMIN_PASS%
echo.
echo 2. Select Realm: '%REALM%'
echo.
echo 3. Navigate to: Realm Settings
echo.
echo 4. Verify Roles Exist:
echo    - Go to 'Realm Roles' tab
echo    - Ensure these roles exist:
echo      * Customer (or CUSTOMER)
echo      * Seller (or SELLER)
echo      * DELIVERY_AGENT
echo.
echo 5. Set Default Role:
echo    - Go to 'Realm Settings' ^> 'User Registration'
echo    - Under 'Default Roles', add: Customer
echo    - Click 'Save'
echo.
echo 6. Enable User Registration (if needed):
echo    - Go to 'Realm Settings' ^> 'Login'
echo    - Enable 'User registration'
echo    - Click 'Save'
echo.
echo 7. Verify Configuration:
echo    - Register a new test user
echo    - Check if 'Customer' role is auto-assigned
echo.
echo ==========================================
echo.
echo For detailed instructions, see:
echo docs\KEYCLOAK_ROLE_CONFIGURATION.md
echo.
pause
