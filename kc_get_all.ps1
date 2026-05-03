$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/eshop/protocol/openid-connect/token" -Method Post -Body @{ grant_type="client_credentials"; client_id="eshop-backend"; client_secret="nUkbQQhPe821Y0roBXu0k73NQk0UVGmP" }
$token = $tokenResponse.access_token

$res = Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/eshop/users" -Headers @{Authorization="Bearer $token"}
$res | ConvertTo-Json | Out-File g:\Project\eshop_back\kc_all_users.json
