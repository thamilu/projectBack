# ============================================================================
# SELLER AUTHENTICATION TEST SCRIPT
# ============================================================================
# 
# Purpose: Test seller authentication in backend by calling seller-protected API
# 
# Prerequisites:
# 1. Backend running on http://localhost:8082
# 2. Keycloak running on http://localhost:8080
# 3. Seller user exists in Keycloak with SELLER role
# 
# Usage:
#   .\test-seller-authentication.ps1
#
# ============================================================================

Write-Host ""
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "  SELLER AUTHENTICATION TEST" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$BACKEND_URL = "http://localhost:8082"
$KEYCLOAK_URL = "http://localhost:8080"
$REALM = "eshop"
$CLIENT_ID = "eshop-client"
$CLIENT_SECRET = "your-client-secret-here"  # Update this

# Seller credentials (update these)
$SELLER_USERNAME = "seller@example.com"
$SELLER_PASSWORD = "password"

Write-Host "[1/4] Testing backend health..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$BACKEND_URL/actuator/health" -Method Get
    Write-Host "✅ Backend is healthy" -ForegroundColor Green
} catch {
    Write-Host "❌ Backend is not responding. Please start the backend." -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/4] Authenticating seller with Keycloak..." -ForegroundColor Yellow

$tokenUrl = "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token"
$tokenBody = @{
    grant_type    = "password"
    client_id     = $CLIENT_ID
    client_secret = $CLIENT_SECRET
    username      = $SELLER_USERNAME
    password      = $SELLER_PASSWORD
    scope         = "openid profile email"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    
    Write-Host "✅ Seller authenticated with Keycloak" -ForegroundColor Green
    Write-Host "   Access Token (first 50 chars): $($accessToken.Substring(0, [Math]::Min(50, $accessToken.Length)))..." -ForegroundColor Gray
    
    # Decode JWT to show roles (simple base64 decode of payload)
    $jwtParts = $accessToken.Split('.')
    if ($jwtParts.Length -ge 2) {
        $payloadBase64 = $jwtParts[1]
        # Add padding if needed
        while ($payloadBase64.Length % 4 -ne 0) {
            $payloadBase64 += "="
        }
        $payloadJson = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payloadBase64))
        $payload = $payloadJson | ConvertFrom-Json
        
        Write-Host "   Username: $($payload.preferred_username)" -ForegroundColor Gray
        Write-Host "   Roles: $($payload.roles -join ', ')" -ForegroundColor Gray
    }
    
} catch {
    Write-Host "❌ Keycloak authentication failed" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "  1. Keycloak is running on $KEYCLOAK_URL" -ForegroundColor Yellow
    Write-Host "  2. Realm '$REALM' exists" -ForegroundColor Yellow
    Write-Host "  3. Client '$CLIENT_ID' is configured" -ForegroundColor Yellow
    Write-Host "  4. Seller credentials are correct" -ForegroundColor Yellow
    Write-Host "  5. CLIENT_SECRET is correct in this script" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "[3/4] Calling SELLER-protected backend API..." -ForegroundColor Yellow

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type"  = "application/json"
}

try {
    $dashboardUrl = "$BACKEND_URL/api/v1/dashboard/seller"
    $dashboardResponse = Invoke-RestMethod -Uri $dashboardUrl -Method Get -Headers $headers
    
    Write-Host "✅ SELLER AUTHENTICATED IN BACKEND!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response from backend:" -ForegroundColor Cyan
    Write-Host ($dashboardResponse | ConvertTo-Json -Depth 5) -ForegroundColor White
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "❌ Backend API call failed with status: $statusCode" -ForegroundColor Red
    
    if ($statusCode -eq 401) {
        Write-Host ""
        Write-Host "Diagnosis: Unauthorized (401)" -ForegroundColor Yellow
        Write-Host "  - JWT token is missing or invalid" -ForegroundColor Yellow
        Write-Host "  - Check if backend JWT issuer-uri matches Keycloak" -ForegroundColor Yellow
    } elseif ($statusCode -eq 403) {
        Write-Host ""
        Write-Host "Diagnosis: Forbidden (403)" -ForegroundColor Yellow
        Write-Host "  - JWT token is valid but user doesn't have SELLER role" -ForegroundColor Yellow
        Write-Host "  - Check Keycloak role mapping" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "Error details: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[4/4] Checking backend logs..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Please check your backend console/logs for:" -ForegroundColor Cyan
Write-Host "  ✅ SELLER AUTHENTICATED | user=$SELLER_USERNAME | roles=..." -ForegroundColor Green
Write-Host ""
Write-Host "If you see this log entry, seller authentication is WORKING! 🎉" -ForegroundColor Green
Write-Host ""

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "  TEST COMPLETED SUCCESSFULLY" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""
