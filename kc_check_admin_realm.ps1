$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/master/protocol/openid-connect/token" -Method Post -Body @{ grant_type="password"; client_id="admin-cli"; username="admin"; password="admin" }
$token = $tokenResponse.access_token

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/eshop-admin/users/fbe3a6b3-4113-4e4f-b097-62dcd4cc1cf2" -Headers @{Authorization="Bearer $token"}
    Write-Host "User found in eshop-admin realm!"
} catch {
    Write-Host "Not found in eshop-admin"
}
