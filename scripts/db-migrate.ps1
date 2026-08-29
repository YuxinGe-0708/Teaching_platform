[CmdletBinding()]
param(
    [string]$ComposeFile = 'docker-compose.yml',
    [string]$Service = 'mysql',
    [string]$Database = 'teaching_platform',
    [string]$Username = 'tp_dev',
    [string]$Password = '123456',
    [string]$MigrationDirectory = 'db/migrations'
)

$ErrorActionPreference = 'Stop'

function Invoke-MySqlSql([string]$Sql) {
    $Sql | docker compose -f $ComposeFile exec -T $Service mysql `
        "-u$Username" "-p$Password" $Database
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed with exit code $LASTEXITCODE"
    }
}

$migrationFiles = Get-ChildItem -LiteralPath $MigrationDirectory -Filter '*.sql' -File |
    Sort-Object Name
if ($migrationFiles.Count -eq 0) {
    Write-Host "No migration files found in $MigrationDirectory"
    exit 0
}

function Test-MigrationTable {
    $exists = (& docker compose -f $ComposeFile exec -T $Service mysql `
        "-u$Username" "-p$Password" -NBe `
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$Database' AND table_name='schema_migrations';" 2>$null).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to query migration table state"
    }
    return $exists -eq '1'
}

foreach ($file in $migrationFiles) {
    $version = $file.BaseName
    $escapedVersion = $version.Replace("'", "''")

    # Bootstrap the bookkeeping table before querying it for the first migration.
    if (-not (Test-MigrationTable)) {
        if ($version -ne '000_schema_migrations') {
            throw "Migration bookkeeping table is missing and $version is not the bootstrap migration"
        }
        Write-Host "Applying bootstrap migration $version"
        $sql = Get-Content -LiteralPath $file.FullName -Raw
        Invoke-MySqlSql $sql
        Invoke-MySqlSql "INSERT INTO schema_migrations (version) VALUES ('$escapedVersion');"
        continue
    }

    $alreadyApplied = (& docker compose -f $ComposeFile exec -T $Service mysql `
        "-u$Username" "-p$Password" $Database -NBe `
        "SELECT COUNT(*) FROM schema_migrations WHERE version='$escapedVersion';" 2>$null).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to query migration state before applying $version"
    }
    if ($alreadyApplied -eq '1') {
        Write-Host "Skipping already applied migration $version"
        continue
    }

    Write-Host "Applying migration $version"
    $sql = Get-Content -LiteralPath $file.FullName -Raw
    Invoke-MySqlSql $sql
    Invoke-MySqlSql "INSERT INTO schema_migrations (version) VALUES ('$escapedVersion');"
}

Write-Host "Database migrations completed successfully."
