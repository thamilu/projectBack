#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Test Seller Dashboard Authentication and Authorization
.DESCRIPTION
    This script validates that the seller dashboard endpoint is properly protected
    and authenticates seller users correctly.
.EXAMPLE
    .\test-seller-dashboard.ps1
#>

$ErrorActionPreference = "Stop"

# Configuration
$BACKEND_URL = "http://localhost:8082"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Seller Dashboard Authentication Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Seller credentials
$sellerUsername = "seller"
$sellerPassword = "seller123"

Write-Host "Step 1: Authenticating seller user..." -ForegroundColor Yellow
Write-Host "  Username: $sellerUsername" -ForegroundColor Gray

try {
    $loginEndpoint = "$BACKEND_URL/api/auth/login"
    
    $body = @{
        username = $sellerUsername
        password = $sellerPassword
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri $loginEndpoint -Method Post -Body $body -ContentType "application/json"
    
    $accessToken = $response.access_token
    
    Write-Host "✅ Authentication successful!" -ForegroundColor Green
    Write-Host ""
    
    # Decode JWT to show roles
    Write-Host "Step 2: Token information..." -ForegroundColor Yellow
    
    Write-Host "  Token Type: $($response.token_type)" -ForegroundColor Gray
    Write-Host "  Expires In: $($response.expires_in) seconds" -ForegroundColor Gray
    
    # Try to decode JWT payload (basic version without full validation)
    if ($accessToken) {
        $tokenParts = $accessToken.Split('.')
        if ($tokenParts.Length -ge 2) {
            $payload = $tokenParts[1]
            
            # Add padding if needed
            $paddingNeeded = 4 - ($payload.Length % 4)
            if ($paddingNeeded -lt 4) {
                $payload = $payload + ("=" * $paddingNeeded)
            }
            
            try {
                $decodedBytes = [System.Convert]::FromBase64String($payload)
                $decodedJson = [System.Text.Encoding]::UTF8.GetString($decodedBytes)
                $tokenData = $decodedJson | ConvertFrom-Json
                
                Write-Host "  User: $($tokenData.preferred_username)" -ForegroundColor Gray
                if ($tokenData.roles) {
                    Write-Host "  Roles: $($tokenData.roles -join ', ')" -ForegroundColor Gray
                }
                Write-Host "  Subject: $($tokenData.sub)" -ForegroundColor Gray
            } catch {
                Write-Host "  (Could not decode token payload)" -ForegroundColor Gray
            }
        }
    }
    Write-Host ""
    
    # Test seller dashboard endpoint
    Write-Host "Step 3: Calling seller dashboard endpoint..." -ForegroundColor Yellow
    Write-Host "  Endpoint: GET $BACKEND_URL/api/v1/dashboard/seller" -ForegroundColor Gray
    
    $headers = @{
        Authorization = "Bearer $accessToken"
        "Content-Type" = "application/json"
    }
    
    $dashboardResponse = Invoke-RestMethod -Uri "$BACKEND_URL/api/v1/dashboard/seller" -Method Get -Headers $headers
    
    Write-Host "✅ Seller dashboard accessed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Cyan
    $dashboardResponse | ConvertTo-Json -Depth 10 | Write-Host
    Write-Host ""
    
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✅ ALL TESTS PASSED" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Expected backend logs:" -ForegroundColor Yellow
    Write-Host "  INFO  - ✅ SELLER authenticated | user=$sellerUsername | roles=[SELLER]" -ForegroundColor Gray
    Write-Host "  URI: /api/v1/dashboard/seller" -ForegroundColor Gray
    Write-Host "  Status: 200" -ForegroundColor Gray
    
} catch {
    Write-Host ""
    Write-Host "❌ TEST FAILED" -ForegroundColor Red
    Write-Host ""
    
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $statusDescription = $_.Exception.Response.StatusDescription
        
        Write-Host "Status Code: $statusCode $statusDescription" -ForegroundColor Red
        
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorBody = $reader.ReadToEnd()
            Write-Host "Error Response:" -ForegroundColor Red
            Write-Host $errorBody -ForegroundColor Red
        } catch {
            Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "  1. Is backend running? Check: $BACKEND_URL/actuator/health" -ForegroundColor Gray
    Write-Host "  2. Are seller credentials correct? (username: seller, password: seller123)" -ForegroundColor Gray
    Write-Host "  3. Is SELLER role assigned in Keycloak?" -ForegroundColor Gray
    Write-Host "  4. Check backend logs for authentication errors" -ForegroundColor Gray
    Write-Host "  5. Try: .\test-keycloak-auth.ps1 to verify Keycloak is working" -ForegroundColor Gray
    
    exit 1
}
