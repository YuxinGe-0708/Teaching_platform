param(
    [string]$MysqlUser = 'root',
    [string]$MysqlPassword = 'root123456'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$files = @(
    (Join-Path $root 'services/user-service/src/main/resources/db/seed-user.sql'),
    (Join-Path $root 'services/learning-service/src/main/resources/db/seed-learning.sql'),
    (Join-Path $root 'services/assessment-service/src/main/resources/db/seed-assessment.sql')
)
foreach ($file in $files) {
    if (-not (Test-Path -LiteralPath $file)) { throw "Seed file not found: $file" }
    Write-Host "Seeding $file"
    Get-Content -LiteralPath $file -Raw |
        docker compose -f (Join-Path $root 'docker-compose.microservices.yml') exec -T microservices-mysql `
            mysql --default-character-set=utf8mb4 "--user=$MysqlUser" "--password=$MysqlPassword"
    if ($LASTEXITCODE -ne 0) { throw "Seed failed: $file" }
}
Write-Host 'Microservice seed data inserted successfully.'
