$mappings = @{
    "com.eshop.app.shared.api.response.CustomerDashboardResponse" = "com.eshop.app.customer.api.response.CustomerDashboardResponse"
    "com.eshop.app.shared.api.response.DeliveryDashboardResponse" = "com.eshop.app.shipping.api.response.DeliveryDashboardResponse"
    "com.eshop.app.shared.application.service.CustomerDashboardService" = "com.eshop.app.customer.application.service.CustomerDashboardService"
    "com.eshop.app.shared.application.service.DeliveryDashboardService" = "com.eshop.app.shipping.application.service.DeliveryDashboardService"
}

$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app
foreach ($file in $files) {
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
        Write-Host "Updated imports in $($file.Name)"
    }
}
