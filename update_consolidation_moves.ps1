$mappings = @{
    "com.eshop.app.processor.SellerModuleProcessor" = "com.eshop.app.seller.application.processor.SellerModuleProcessor"
    "com.eshop.app.strategy.SellerRegistrationValidator" = "com.eshop.app.seller.application.strategy.SellerRegistrationValidator"
    "com.eshop.app.filter.RequestLoggingFilter" = "com.eshop.app.core.web.filter.RequestLoggingFilter"
    "com.eshop.app.web.RequestCorrelationFilter" = "com.eshop.app.core.web.filter.RequestCorrelationFilter"
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
