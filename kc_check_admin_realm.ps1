$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/master/protocol/openid-connect/token" -Method Post -Body @{ grant_type="password"; client_id="admin-cli"; username="admin"; password="Admin@@Secret123" }
$token = $tokenResponse.access_token

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/eshop-admin/clients" -Headers @{Authorization="Bearer $token"}
    Write-Host "Clients list in eshop-admin:"
    $res | ForEach-Object { Write-Host "- ClientID: $($_.clientId), Enabled: $($_.enabled), Public: $($_.publicClient), Redirects: $($_.redirectUris)" }
} catch {
    Write-Host "Error listing clients in eshop-admin: $($_.Exception.Message)"
}
