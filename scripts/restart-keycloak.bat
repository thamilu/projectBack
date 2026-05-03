@echo off
echo ==========================================
echo Keycloak Restart Script
echo ==========================================
echo.
echo This will restart Keycloak with the fixed configuration
echo.
pause

cd /d "%~dp0.."

echo.
echo [1/3] Stopping Keycloak...
docker-compose -f docker-compose-dev.yml stop keycloak

echo.
echo [2/3] Removing Keycloak container to force re-import...
docker-compose -f docker-compose-dev.yml rm -f keycloak

echo.
echo [3/3] Starting Keycloak...
docker-compose -f docker-compose-dev.yml up -d keycloak

echo.
echo ==========================================
echo Waiting for Keycloak to start (30 sec)...
echo ==========================================
timeout /t 30 /nobreak

echo.
echo ✅ Done!
echo.
echo Access Keycloak Admin Console:
echo   URL: http://localhost:8080
echo   Username: admin
echo   Password: Admin@@Secret123
echo.
echo Test the fix:
echo   1. Register a new user
echo   2. Check if CUSTOMER role is assigned
echo.
pause
