$files = Get-ChildItem -Recurse -Include *.java -Path g:\Project\eshop_back\src\main\java\com\eshop\app
foreach ($file in $files) {
    $content = Get-Content $file.FullName
    $modified = $false
    
    # Update package declarations for moved files
    if ($file.FullName -match "user\\infrastructure\\config") {
        if ($content -match "package com.eshop.app.config;") {
            $content = $content -replace "package com.eshop.app.config;", "package com.eshop.app.user.infrastructure.config;"
            $modified = $true
        }
    }
    if ($file.FullName -match "payment\\infrastructure\\config") {
        if ($content -match "package com.eshop.app.config;") {
            $content = $content -replace "package com.eshop.app.config;", "package com.eshop.app.payment.infrastructure.config;"
            $modified = $true
        }
    }
    if ($file.FullName -match "location\\infrastructure\\config") {
        if ($content -match "package com.eshop.app.config;") {
            $content = $content -replace "package com.eshop.app.config;", "package com.eshop.app.location.infrastructure.config;"
            $modified = $true
        }
    }

    if ($modified) {
        Set-Content $file.FullName $content
        Write-Host "Updated package in $($file.Name)"
    }
}
