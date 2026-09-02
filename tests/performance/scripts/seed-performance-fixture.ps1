[CmdletBinding()]
param(
    [ValidateSet("monolith", "microservices")]
    [string]$Target = "microservices",
    [string]$ContainerName = "",
    [string]$MysqlPassword = "root123456"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$sqlFile = Join-Path $repoRoot "tests\performance\data\seed-$Target.sql"

if (-not $ContainerName) {
    if ($Target -eq "monolith") {
        $ContainerName = "teachplatform-mysql"
    } else {
        $ContainerName = "teaching_platform-microservices-mysql-1"
    }
}

if (-not (Test-Path -LiteralPath $sqlFile)) {
    throw "Fixture file not found: $sqlFile"
}

docker inspect $ContainerName *> $null
if ($LASTEXITCODE -ne 0) {
    throw "MySQL container is not running: $ContainerName"
}

Get-Content -Raw -LiteralPath $sqlFile | docker exec -i $ContainerName `
    mysql --default-character-set=utf8mb4 "-uroot" "-p$MysqlPassword"
if ($LASTEXITCODE -ne 0) {
    throw "Performance fixture import failed."
}

Write-Host "Performance fixture imported into ${Target}: $ContainerName"
