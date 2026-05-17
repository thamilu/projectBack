# Update packages in moved files
$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app\shipping\api\response
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    $content = $content -replace "package com.eshop.app.shared.api.response;", "package com.eshop.app.shipping.api.response;"
    Set-Content $file.FullName $content
}

$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app\location\api\response
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    $content = $content -replace "package com.eshop.app.shared.api.response;", "package com.eshop.app.location.api.response;"
    Set-Content $file.FullName $content
}

$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app\user\api\response
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    $content = $content -replace "package com.eshop.app.shared.api.response;", "package com.eshop.app.user.api.response;"
    Set-Content $file.FullName $content
}

# Update imports globally
$mappings = @{
    "com.eshop.app.shared.api.response.ShippingResponse" = "com.eshop.app.shipping.api.response.ShippingResponse"
    "com.eshop.app.shared.api.response.LocalityDTO" = "com.eshop.app.location.api.response.LocalityDTO"
    "com.eshop.app.shared.api.response.LocationPricingDto" = "com.eshop.app.location.api.response.LocationPricingDto"
    "com.eshop.app.shared.api.response.LocationResponseDTO" = "com.eshop.app.location.api.response.LocationResponseDTO"
    "com.eshop.app.shared.api.response.PickupLocationDto" = "com.eshop.app.location.api.response.PickupLocationDto"
    "com.eshop.app.shared.api.response.PincodeDTO" = "com.eshop.app.location.api.response.PincodeDTO"
    "com.eshop.app.shared.api.response.AuthResponse" = "com.eshop.app.user.api.response.AuthResponse"
    "com.eshop.app.shared.api.response.TokenResponse" = "com.eshop.app.user.api.response.TokenResponse"
    "com.eshop.app.shared.api.response.SessionResponse" = "com.eshop.app.user.api.response.SessionResponse"
    "com.eshop.app.shared.api.response.TwoFactorSetupResponse" = "com.eshop.app.user.api.response.TwoFactorSetupResponse"
}

$allFiles = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app
foreach ($file in $allFiles) {
    $content = Get-Content $file.FullName
    $modified = $false
    foreach ($old in $mappings.Keys) {
        if ($content -match [regex]::Escape($old)) {
            $content = $content -replace [regex]::Escape($old), $mappings[$old]
            $modified = $true
        }
    }
    if ($modified) {
        Set-Content $file.FullName $content
    }
}
