param(
    [Parameter(Mandatory=$false)]
    [string]$Username = "root",

    [Parameter(Mandatory=$false)]
    [string]$Password = "123456",

    [Parameter(Mandatory=$false)]
    [string]$Database = "teaching_platform",

    [Parameter(Mandatory=$false)]
    [string]$MigrationsPath = "./db"
)

$ErrorActionPreference = "Stop"

Write-Host "==> Starting database migration for database: $Database..."

# 1. 创建迁移版本记录表并初始化基础版本号
$initLedgerSql = @"
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(255) NOT NULL PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT IGNORE INTO schema_migrations (version) VALUES ('000_schema_migrations');
"@

& docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database -e "$initLedgerSql"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to initialize schema_migrations ledger."
}
Write-Host "==> schema_migrations ledger table verified."

# 2. 扫描并执行 db 目录下的所有 SQL 迁移文件
if (Test-Path $MigrationsPath) {
    $sqlFiles = Get-ChildItem -Path $MigrationsPath -Filter "*.sql" -Recurse | Sort-Object Name
    foreach ($file in $sqlFiles) {
        $versionName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)

        # 检查该脚本是否已执行过
        $checkSql = "SELECT version FROM schema_migrations WHERE version='$versionName';"
        $applied = (& docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database -NBe "$checkSql").Trim()

        if (-not $applied) {
            Write-Host "==> Applying migration: $($file.Name)..."
            Get-Content -Raw -Encoding UTF8 $file.FullName | & docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database
            if ($LASTEXITCODE -ne 0) {
                throw "Migration failed for file: $($file.FullName)"
            }

            # 记录迁移版本
            $recordSql = "INSERT INTO schema_migrations (version) VALUES ('$versionName');"
            & docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database -e "$recordSql"
            Write-Host "==> Migration $($file.Name) applied successfully."
        } else {
            Write-Host "==> Migration $($file.Name) already applied, skipping."
        }
    }
}

Write-Host "==> All database migrations completed successfully!"