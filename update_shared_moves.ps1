$mappings = @{
    "com.eshop.app.shared.api.controller.LocationController" = "com.eshop.app.location.api.controller.LocationController"
    "com.eshop.app.shared.application.service.LocationService" = "com.eshop.app.location.application.service.LocationService"
    "com.eshop.app.shared.api.controller.ShippingController" = "com.eshop.app.shipping.api.controller.ShippingController"
    "com.eshop.app.shared.api.controller.DeliveryController" = "com.eshop.app.shipping.api.controller.DeliveryController"
    "com.eshop.app.shared.application.service.ShippingService" = "com.eshop.app.shipping.application.service.ShippingService"
    "com.eshop.app.shared.application.service.DeliveryAgentService" = "com.eshop.app.shipping.application.service.DeliveryAgentService"
    "com.eshop.app.shared.api.controller.KeycloakAuthController" = "com.eshop.app.user.api.controller.KeycloakAuthController"
    "com.eshop.app.shared.application.service.KeycloakAuthService" = "com.eshop.app.user.application.service.KeycloakAuthService"
    "com.eshop.app.shared.application.service.KeycloakService" = "com.eshop.app.user.application.service.KeycloakService"
    "com.eshop.app.shared.api.controller.HomeController" = "com.eshop.app.catalog.api.controller.HomeController"
    "com.eshop.app.shared.application.service.HomeService" = "com.eshop.app.catalog.application.service.HomeService"
    "com.eshop.app.shared.api.controller.MeController" = "com.eshop.app.user.api.controller.MeController"
    "com.eshop.app.shared.api.controller.SessionController" = "com.eshop.app.user.api.controller.SessionController"
    "com.eshop.app.shared.application.service.ActivityService" = "com.eshop.app.user.application.service.ActivityService"
    "com.eshop.app.shared.application.service.QuickStatsService" = "com.eshop.app.user.application.service.QuickStatsService"
    "com.eshop.app.shared.application.service.SecureFileUploadService" = "com.eshop.app.storage.application.service.SecureFileUploadService"
}

$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    $modified = $false
    
    # 1. Update Package Declaration
    foreach ($oldFQN in $mappings.Keys) {
        $className = $oldFQN.Split('.')[-1]
        $oldPkg = $oldFQN.Substring(0, $oldFQN.Length - $className.Length - 1)
        $newPkg = $mappings[$oldFQN].Substring(0, $mappings[$oldFQN].Length - $className.Length - 1)
        
        if ($file.Name -eq "$className.java") {
            if ($content -match "package $oldPkg;") {
                $content = $content -replace "package $oldPkg;", "package $newPkg;"
                $modified = $true
            }
        }
    }
    
    # 2. Update Imports
    foreach ($oldFQN in $mappings.Keys) {
        $newFQN = $mappings[$oldFQN]
        if ($content -match "import $oldFQN;") {
            $content = $content -replace "import $oldFQN;", "import $newFQN;"
            $modified = $true
        }
    }

    if ($modified) {
        Set-Content $file.FullName $content
        Write-Host "Updated $($file.Name)"
    }
}
