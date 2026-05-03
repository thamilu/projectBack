# Test the /api/me endpoint
# Usage: .\test-api-me.ps1 "your-access-token-here"

param(
    [Parameter(Mandatory=$false)]
    [string]$AccessToken
)

$baseUrl = "http://localhost:8080"
$endpoint = "/api/me"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Backend /api/me Endpoint" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if token provided
if ([string]::IsNullOrEmpty($AccessToken)) {
    Write-Host "⚠️  No access token provided!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To test this endpoint, you need a valid JWT access token from Keycloak." -ForegroundColor White
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor Green
    Write-Host "  .\test-api-me.ps1 `"your-access-token-here`"" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To get an access token:" -ForegroundColor Green
    Write-Host "  1. Run: .\test-keycloak-auth.ps1 (if available)" -ForegroundColor Gray
    Write-Host "  2. Or login through your frontend and copy the token from browser DevTools" -ForegroundColor Gray
    Write-Host "  3. Or use Keycloak Direct Access Grant (see below)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Example using curl to get token:" -ForegroundColor Green
    Write-Host "  curl -X POST http://localhost:8080/realms/eshop/protocol/openid-connect/token ``" -ForegroundColor Gray
    Write-Host "    -H `"Content-Type: application/x-www-form-urlencoded`" ``" -ForegroundColor Gray
    Write-Host "    -d `"client_id=eshop-client`" ``" -ForegroundColor Gray
    Write-Host "    -d `"grant_type=password`" ``" -ForegroundColor Gray
    Write-Host "    -d `"username=your-username`" ``" -ForegroundColor Gray
    Write-Host "    -d `"password=your-password`"" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Host "🔍 Testing endpoint: $baseUrl$endpoint" -ForegroundColor White
Write-Host "🔑 Using Bearer token: ${AccessToken.Substring(0, [Math]::Min(20, $AccessToken.Length))}..." -ForegroundColor White
Write-Host ""

try {
    # Test without authentication (should fail with 401)
    Write-Host "Test 1: Without Authentication (should return 401)" -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl$endpoint" -Method Get -ErrorAction Stop
        Write-Host "❌ UNEXPECTED: Endpoint should require authentication!" -ForegroundColor Red
        Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Red
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) {
            Write-Host "✅ PASS: Got expected 401 Unauthorized" -ForegroundColor Green
        } else {
            Write-Host "❌ FAIL: Got unexpected error: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        }
    }
    Write-Host ""

    # Test with authentication (should succeed)
    Write-Host "Test 2: With Bearer Token (should return 200 with user info)" -ForegroundColor Yellow
    try {
        $headers = @{
            "Authorization" = "Bearer $AccessToken"
            "Content-Type" = "application/json"
        }
        
        $response = Invoke-WebRequest -Uri "$baseUrl$endpoint" -Method Get -Headers $headers -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ PASS: Got 200 OK" -ForegroundColor Green
            Write-Host ""
            Write-Host "Response Body:" -ForegroundColor Cyan
            $json = $response.Content | ConvertFrom-Json
            Write-Host "  Sub (User ID): $($json.sub)" -ForegroundColor White
            Write-Host "  Email: $($json.email)" -ForegroundColor White
            Write-Host "  Roles: $($json.roles | ConvertTo-Json -Compress)" -ForegroundColor White
        } else {
            Write-Host "❌ FAIL: Expected 200 but got $($response.StatusCode)" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ FAIL: Request failed" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            Write-Host "   Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        }
    }
    Write-Host ""

    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Test Complete!" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

} catch {
    Write-Host "❌ Test failed with error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
