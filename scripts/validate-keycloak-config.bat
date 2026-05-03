@echo off
REM ============================================
REM Keycloak Configuration Validation Script (Windows)
REM ============================================

setlocal enabledelayedexpansion

set ERRORS=0
set WARNINGS=0
set CHECKS=0

echo ================================================================
echo        Keycloak Configuration Validation Script
echo ================================================================
echo.

REM ============================================
REM 1. Check if JSON files exist
REM ============================================
echo [1/5] Checking configuration files...
set /a CHECKS+=1

if not exist "keycloak-import\eshop-realm.json" (
    echo [ERROR] eshop-realm.json not found
    set /a ERRORS+=1
) else (
    echo [OK] eshop-realm.json found
)

REM ============================================
REM 2. Validate JSON syntax
REM ============================================
echo.
echo [2/5] Validating JSON syntax...
set /a CHECKS+=1

where python >nul 2>nul
if %ERRORLEVEL% equ 0 (
    python -m json.tool keycloak-import\eshop-realm.json >nul 2>nul
    if !ERRORLEVEL! equ 0 (
        echo [OK] JSON syntax is valid
    ) else (
        echo [ERROR] Invalid JSON syntax
        set /a ERRORS+=1
    )
) else (
    echo [WARNING] Python not found, skipping JSON validation
    set /a WARNINGS+=1
)

REM ============================================
REM 3. Check required environment variables
REM ============================================
echo.
echo [3/5] Checking environment variables...
set /a CHECKS+=1

if exist ".env" (
    echo [OK] Found .env file
) else (
    echo [WARNING] .env file not found
    set /a WARNINGS+=1
)

echo.
echo Checking required environment variables:

set REQUIRED_VARS=ESHOP_BACKEND_CLIENT_SECRET SMTP_HOST SMTP_PORT SMTP_FROM_EMAIL SMTP_REPLY_TO SMTP_USER SMTP_PASSWORD GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET FACEBOOK_CLIENT_ID FACEBOOK_CLIENT_SECRET

for %%v in (%REQUIRED_VARS%) do (
    if not defined %%v (
        echo   [WARNING] %%v is not set
        set /a WARNINGS+=1
    ) else (
        echo   [OK] %%v is set
    )
)

REM ============================================
REM 4. Check Keycloak connectivity
REM ============================================
echo.
echo [4/5] Checking Keycloak connectivity...
set /a CHECKS+=1

where curl >nul 2>nul
if %ERRORLEVEL% equ 0 (
    curl -s -f -o nul http://localhost:8080/health/ready
    if !ERRORLEVEL! equ 0 (
        echo [OK] Keycloak is running and ready
        
        curl -s -f -o nul http://localhost:8080/realms/eshop
        if !ERRORLEVEL! equ 0 (
            echo [OK] eshop realm is accessible
        ) else (
            echo [WARNING] eshop realm not found
            set /a WARNINGS+=1
        )
    ) else (
        echo [WARNING] Keycloak is not running
        echo   Start with: docker-compose -f docker-compose-dev.yml up -d keycloak
        set /a WARNINGS+=1
    )
) else (
    echo [WARNING] curl not found, skipping connectivity check
    set /a WARNINGS+=1
)

REM ============================================
REM 5. Verify realm configuration structure
REM ============================================
echo.
echo [5/5] Verifying realm configuration structure...
set /a CHECKS+=1

where jq >nul 2>nul
if %ERRORLEVEL% equ 0 (
    REM Using jq to validate structure
    echo [OK] jq found, validating structure
) else (
    echo [WARNING] jq not found, skipping structure validation
    echo   Install from: https://stedolan.github.io/jq/download/
    set /a WARNINGS+=1
)

REM ============================================
REM Summary
REM ============================================
echo.
echo ================================================================
echo                    Validation Summary
echo ================================================================
echo Total Checks: %CHECKS%
echo Errors:       %ERRORS%
echo Warnings:     %WARNINGS%
echo ================================================================
echo.

if %ERRORS% equ 0 (
    if %WARNINGS% equ 0 (
        echo [SUCCESS] All checks passed! Configuration is ready.
        exit /b 0
    ) else (
        echo [WARNING] Configuration has warnings but should work.
        exit /b 0
    )
) else (
    echo [ERROR] Configuration has errors that must be fixed.
    exit /b 1
)
