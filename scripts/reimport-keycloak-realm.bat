@echo off
REM Re-import Keycloak Realm with Fixed Default Roles

echo ==========================================
echo Keycloak Realm Re-Import Script
echo ==========================================
echo.

echo This will re-import the realm configuration with fixed default roles.
echo.
echo IMPORTANT: This will override current realm settings!
echo.
pause

echo.
echo Step 1: Stopping Keycloak...
docker-compose stop keycloak

echo.
echo Step 2: Starting Keycloak with realm import...
docker-compose up -d keycloak

echo.
echo Step 3: Waiting for Keycloak to start (30 seconds)...
timeout /t 30 /nobreak

echo.
echo Step 4: Checking Keycloak status...
docker logs keycloak --tail 50

echo.
echo ==========================================
echo Realm re-import complete!
echo ==========================================
echo.
echo Next steps:
echo 1. Open Keycloak Admin Console: http://localhost:8080
echo 2. Login with admin/admin
echo 3. Select 'eshop' realm
echo 4. Go to: Realm Settings ^> User Registration ^> Default roles
echo 5. Verify 'Customer' role is in the list
echo.
echo To test:
echo 1. Register a new user
echo 2. Check if Customer role is assigned
echo.
pause
