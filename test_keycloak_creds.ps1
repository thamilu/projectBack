$clientId = "eshop-backend"
$clientSecret = "oReCVQRonvdxXtImvKJyWUsGpJPfVEVj"
$tokenUrl = "http://localhost:8080/realms/eshop/protocol/openid-connect/token"

Write-Host "Testing Keycloak Connection..."
Write-Host "Client ID: $clientId"
Write-Host "URL: $tokenUrl"

try {
    $response = Invoke-RestMethod -Method Post -Uri $tokenUrl -Body @{
        client_id     = $clientId
        client_secret = $clientSecret
        grant_type    = "client_credentials"
    }
    Write-Host "SUCCESS! Received Access Token."
    Write-Host "Token Type: $($response.token_type)"
    Write-Host "Expires In: $($response.expires_in)"
} catch {
    Write-Host "FAILED to get token." -ForegroundColor Red
    Write-Host "Error Details:"
    $_.Exception.Response
    
    # Read error stream if available
    if ($_.Exception.Response.GetResponseStream()) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $body = $reader.ReadToEnd()
        Write-Host "Body: $body" -ForegroundColor Yellow
    }
}
