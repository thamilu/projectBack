$oldPackage = "com.eshop.app.shared.api.response.WishlistResponse"
$newPackage = "com.eshop.app.customer.api.response.WishlistResponse"
$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    if ($content -match $oldPackage) {
        $content = $content -replace $oldPackage, $newPackage
        Set-Content $file.FullName $content
        Write-Host "Updated imports in $($file.Name)"
    }
}

$oldPackage = "com.eshop.app.shared.domain.entity.Wishlist"
$newPackage = "com.eshop.app.customer.domain.entity.Wishlist"
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    if ($content -match $oldPackage) {
        $content = $content -replace $oldPackage, $newPackage
        Set-Content $file.FullName $content
        Write-Host "Updated imports in $($file.Name)"
    }
}

$oldPackage = "com.eshop.app.shared.domain.repository.WishlistRepository"
$newPackage = "com.eshop.app.customer.infrastructure.persistence.WishlistRepository"
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    if ($content -match $oldPackage) {
        $content = $content -replace $oldPackage, $newPackage
        Set-Content $file.FullName $content
        Write-Host "Updated imports in $($file.Name)"
    }
}

$oldPackage = "com.eshop.app.shared.application.service.WishlistService"
$newPackage = "com.eshop.app.customer.application.port.in.WishlistService"
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    if ($content -match $oldPackage) {
        $content = $content -replace $oldPackage, $newPackage
        Set-Content $file.FullName $content
        Write-Host "Updated imports in $($file.Name)"
    }
}

$oldPackage = "com.eshop.app.shared.application.mapper.WishlistMapper"
$newPackage = "com.eshop.app.customer.application.mapper.WishlistMapper"
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    if ($content -match $oldPackage) {
        $content = $content -replace $oldPackage, $newPackage
        Set-Content $file.FullName $content
        Write-Host "Updated imports in $($file.Name)"
    }
}
