$mappings = @{
    "com.eshop.app.validation" = "com.eshop.app.core.validation"
    "com.eshop.app.specification.ProductSpecificationBuilder" = "com.eshop.app.catalog.application.specification.ProductSpecificationBuilder"
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
