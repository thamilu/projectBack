$javaBase = (Get-Item "G:\Project\eshop_back\src\main\java").FullName
Get-ChildItem -Path $javaBase -Recurse -Filter *.java | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    $dir = [System.IO.Path]::GetDirectoryName($_.FullName)
    if ($dir.Length -gt $javaBase.Length) {
        $expectedPackage = $dir.Substring($javaBase.Length + 1).Replace('\', '.')
        if ($content -notmatch "package $expectedPackage;") {
            $newContent = $content -replace '(?m)^\s*package\s+[^;]+;', "package $expectedPackage;"
            if ($newContent -eq $content) {
                 # If no package declaration was found, prepend it
                 $newContent = "package $expectedPackage;`r`n`r`n" + $content.TrimStart()
            }
            [System.IO.File]::WriteAllText($_.FullName, $newContent)
            Write-Output "Fixed package in: $($_.FullName) -> $expectedPackage"
        }
    }
}
