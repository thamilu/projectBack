$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/eshop/protocol/openid-connect/token" -Method Post -Body @{ grant_type="client_credentials"; client_id="eshop-backend"; client_secret="nUkbQQhPe821Y0roBXu0k73NQk0UVGmP" }
$token = $tokenResponse.access_token

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/eshop/roles/CUSTOMER" -Headers @{Authorization="Bearer $token"}
    Write-Host "Role CUSTOMER found"
} catch {
    Write-Host "Role Error: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Role Error MSG: $($_.Exception.Message)"
}

try {
    Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/eshop/users/fbe3a6b3-4113-4e4f-b097-62dcd4cc1cf2/role-mappings/realm" -Headers @{Authorization="Bearer $token"}
    Write-Host "User fbe3... found"
} catch {
    Write-Host "User Error: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "User Error MSG: $($_.Exception.Message)"
}
try {
    Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/eshop/users?username=admin" -Headers @{Authorization="Bearer $token"}
    Write-Host "User Admin search successful"
} catch {
    Write-Host "Admin Search Error: $($_.Exception.Message)"
}
