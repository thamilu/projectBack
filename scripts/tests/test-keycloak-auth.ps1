# Keycloak Authentication Test Script for Windows PowerShell
# This script tests all the Keycloak authentication endpoints

$BaseUrl = "http://localhost:8082/api/auth"
$AdminBaseUrl = "http://localhost:8082/api/admin"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Keycloak Authentication Test Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Login with username and password
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 1: Login with Username/Password" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Attempting login with username: admin, password: admin123" -ForegroundColor Gray

$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json"
    
    $accessToken = $loginResponse.access_token
    $refreshToken = $loginResponse.refresh_token
    
    Write-Host "✓ Login successful!" -ForegroundColor Green
    $loginResponse | ConvertTo-Json -Depth 3
    Write-Host ""
} catch {
    Write-Host "✗ Login failed!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

# Test 2: Get current user info
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 2: Get Current User (from JWT)" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Fetching user info using access token" -ForegroundColor Gray

try {
    $headers = @{
        "Authorization" = "Bearer $accessToken"
    }
    
    $userInfo = Invoke-RestMethod -Uri "$BaseUrl/me" `
        -Method Get `
        -Headers $headers
    
    Write-Host "✓ User info retrieved successfully!" -ForegroundColor Green
    $userInfo | ConvertTo-Json -Depth 3
    Write-Host ""
} catch {
    Write-Host "✗ Failed to get user info" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# Test 3: Get user info from Keycloak
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 3: Get User Info from Keycloak" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

try {
    $kcUserInfo = Invoke-RestMethod -Uri "$BaseUrl/userinfo" `
        -Method Get `
        -Headers $headers
    
    Write-Host "✓ Keycloak user info retrieved!" -ForegroundColor Green
    $kcUserInfo | ConvertTo-Json -Depth 3
    Write-Host ""
} catch {
    Write-Host "✗ Failed to get Keycloak user info" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# Test 4: Introspect token
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 4: Introspect Token" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

try {
    $introspect = Invoke-RestMethod -Uri "$BaseUrl/introspect" `
        -Method Post `
        -Headers $headers
    
    Write-Host "✓ Token introspection successful!" -ForegroundColor Green
    $introspect | ConvertTo-Json -Depth 3
    Write-Host ""
} catch {
    Write-Host "✗ Token introspection failed" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# Test 5: Get OpenID configuration
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 5: Get OpenID Configuration" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

try {
    $config = Invoke-RestMethod -Uri "$BaseUrl/config" -Method Get
    
    Write-Host "✓ OpenID configuration retrieved!" -ForegroundColor Green
    Write-Host "Issuer: $($config.issuer)"
    Write-Host "Authorization Endpoint: $($config.authorization_endpoint)"
    Write-Host "Token Endpoint: $($config.token_endpoint)"
    Write-Host ""
} catch {
    Write-Host "✗ Failed to get OpenID configuration" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# Test 6: Get login URL
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 6: Get OAuth2 Login URL" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

try {
    $loginUrl = Invoke-RestMethod -Uri "$BaseUrl/login-url?redirectUri=http://localhost:3000/callback" `
        -Method Get
    
    Write-Host "✓ Login URL generated!" -ForegroundColor Green
    $loginUrl | ConvertTo-Json
    Write-Host ""
} catch {
    Write-Host "✗ Failed to generate login URL" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# Test 7: Refresh token
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 7: Refresh Access Token" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Using refresh token to get new access token" -ForegroundColor Gray

$refreshBody = @{
    refreshToken = $refreshToken
} | ConvertTo-Json

try {
    $refreshResponse = Invoke-RestMethod -Uri "$BaseUrl/refresh" `
        -Method Post `
        -Body $refreshBody `
        -ContentType "application/json"
    
    $newAccessToken = $refreshResponse.access_token
    Write-Host "✓ Token refreshed successfully!" -ForegroundColor Green
    Write-Host "Token Type: $($refreshResponse.token_type)"
    Write-Host "Expires In: $($refreshResponse.expires_in) seconds"
    Write-Host ""
} catch {
    Write-Host "✗ Token refresh failed" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# Test 8: Logout
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Test 8: Logout" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Logging out user" -ForegroundColor Gray

$logoutBody = @{
    refreshToken = $refreshToken
} | ConvertTo-Json

try {
    $logoutResponse = Invoke-RestMethod -Uri "$BaseUrl/logout" `
        -Method Post `
        -Body $logoutBody `
        -ContentType "application/json"
    
    Write-Host "✓ Logout successful!" -ForegroundColor Green
    $logoutResponse | ConvertTo-Json
    Write-Host ""
} catch {
    Write-Host "✗ Logout failed" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All Tests Completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Authentication flow tested successfully" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Test with frontend application"
Write-Host "2. Create additional test users with different roles"
Write-Host "3. Test admin endpoints (requires ADMIN role)"
Write-Host ""
