$movedFiles = @(
    "KeycloakAdminClientConfig", "KeycloakConfig", "KeycloakHealthIndicator", "KeycloakHealthIndicatorAutoConfig", "CustomKeycloakJacksonProvider", "DefaultJwtDecoderConfig", "KeycloakProperties", "JwtProperties", "KeycloakConfigProperties"
)
$paymentFiles = @("StripeConfigValidator", "StripeProperties", "RazorpayProperties", "PaypalProperties", "PayuProperties", "CashfreeProperties", "EmiProperties", "UpiProperties", "PaymentProperties")
$locationFiles = @("GoogleProperties", "GoogleMapsConfig", "LocationSearchProperties")

$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    $modified = $false
    
    foreach ($className in $movedFiles) {
        $oldImport = "import com.eshop.app.config.$className;"
        $newImport = "import com.eshop.app.user.infrastructure.config.$className;"
        if ($content -match [regex]::Escape($oldImport)) {
            $content = $content -replace [regex]::Escape($oldImport), $newImport
            $modified = $true
        }
    }
    foreach ($className in $paymentFiles) {
        $oldImport = "import com.eshop.app.config.$className;"
        $newImport = "import com.eshop.app.payment.infrastructure.config.$className;"
        if ($content -match [regex]::Escape($oldImport)) {
            $content = $content -replace [regex]::Escape($oldImport), $newImport
            $modified = $true
        }
    }
    foreach ($className in $locationFiles) {
        $oldImport = "import com.eshop.app.config.$className;"
        $newImport = "import com.eshop.app.location.infrastructure.config.$className;"
        if ($content -match [regex]::Escape($oldImport)) {
            $content = $content -replace [regex]::Escape($oldImport), $newImport
            $modified = $true
        }
    }

    if ($modified) {
        Set-Content $file.FullName $content
        Write-Host "Updated imports in $($file.Name)"
    }
}
