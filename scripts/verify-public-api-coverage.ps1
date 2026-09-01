$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $repoRoot "docs/testing/public-api-coverage.csv"
$manifest = Import-Csv -LiteralPath $manifestPath
$declared = [System.Collections.Generic.List[string]]::new()

Get-ChildItem (Join-Path $repoRoot "services") -Recurse -File -Filter "*Controller.java" | ForEach-Object {
    $relative = $_.FullName.Substring($repoRoot.Length + 1)
    if ($relative -match '[\\/]controller[\\/]internal[\\/]') { return }
    $service = ($relative -split '[\\/]')[1]
    $source = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
    $classBaseMatch = [regex]::Match($source, '@RequestMapping\s*\(\s*"([^"]+)"\s*\)\s*\r?\n\s*public\s+class')
    $base = if ($classBaseMatch.Success) { $classBaseMatch.Groups[1].Value } else { "" }
    $routeMatches = [regex]::Matches($source, '@(Get|Post|Put|Delete|Patch)Mapping(?:\s*\(\s*"([^"]*)"[^)]*\))?')
    foreach ($route in $routeMatches) {
        $path = "$base$($route.Groups[2].Value)"
        if ($path.StartsWith('/api/')) {
            $declared.Add("$service|$($route.Groups[1].Value.ToUpper())|$path")
        }
    }
}

$covered = $manifest | ForEach-Object { "$($_.service)|$($_.method.ToUpper())|$($_.path)" }
$missing = @($declared | Sort-Object -Unique | Where-Object { $_ -notin $covered })
$stale = @($covered | Sort-Object -Unique | Where-Object { $_ -notin $declared })
$invalid = @($manifest | Where-Object { [string]::IsNullOrWhiteSpace($_.use_case) -or [string]::IsNullOrWhiteSpace($_.automated_test) })
if ($missing.Count -or $stale.Count -or $invalid.Count) {
    if ($missing.Count) { Write-Error "Public endpoints missing API tests:`n$($missing -join "`n")" }
    if ($stale.Count) { Write-Error "Stale API coverage rows:`n$($stale -join "`n")" }
    if ($invalid.Count) { Write-Error "Coverage rows must name use_case and automated_test." }
    exit 1
}
Write-Host "Public API coverage gate passed: $($declared.Count) controller mappings / $($manifest.Count) test rows."
