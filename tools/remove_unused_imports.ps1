$root = "src"
$removed = 0
Get-ChildItem -Path $root -Recurse -Filter *.java | ForEach-Object {
    $path = $_.FullName
    $lines = Get-Content -Encoding UTF8 $path -ErrorAction Stop -WarningAction SilentlyContinue
    $importLines = @()
    for ($i=0;$i -lt $lines.Count;$i++){
        $l = $lines[$i]
        if ($l.TrimStart() -like 'import *' -and -not $l.TrimStart().StartsWith('import static')){
            $importLines += @{idx=$i; line=$l}
        }
    }
    if ($importLines.Count -eq 0) { continue }
    $toRemove = @()
    foreach ($imp in $importLines) {
        if ($imp.line -match '\s*import\s+([\w\.]+)\.(\w+)\s*;'){
            $simple = $matches[2]
            # build remaining text without this import line
            $remaining = $lines | Where-Object {$_ -ne $imp.line} | Out-String
            if (-not ([regex]::IsMatch($remaining, "\b" + [regex]::Escape($simple) + "\b"))) {
                $toRemove += $imp.idx
            }
        }
    }
    if ($toRemove.Count -gt 0) {
        $newLines = @()
        for ($i=0;$i -lt $lines.Count;$i++){
            if ($toRemove -contains $i) { continue }
            $newLines += $lines[$i]
        }
        $newLines -join "`n" | Set-Content -Encoding UTF8 $path
        $removed += $toRemove.Count
        Write-Host "Removed $($toRemove.Count) imports from $path"
    }
}
Write-Host "DONE. Total removed: $removed"