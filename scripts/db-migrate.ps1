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

# 2. 将 db/init 中的初始 SQL 标记为已应用（避免重复执行冲突）
$initPath = Join-Path $MigrationsPath "init"
if (Test-Path $initPath) {
    $initFiles = Get-ChildItem -Path $initPath -Filter "*.sql"
    foreach ($file in $initFiles) {
        $initVersion = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
        $recordSql = "INSERT IGNORE INTO schema_migrations (version) VALUES ('$initVersion');"
        & docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database -e "$recordSql"
    }
}

# 3. 扫描并执行除 init 目录外的所有增量迁移脚本
if (Test-Path $MigrationsPath) {
    # 排除 init 目录，仅获取需要增量迁移的文件
    $sqlFiles = Get-ChildItem -Path $MigrationsPath -Filter "*.sql" -Recurse | Where-Object { $_.DirectoryName -notmatch '[\\/]init$' } | Sort-Object Name
    foreach ($file in $sqlFiles) {
        $versionName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)

        # 检查该脚本是否已执行过
        $checkSql = "SELECT version FROM schema_migrations WHERE version='$versionName';"
        $appliedResult = & docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database -NBe "$checkSql"

        $applied = if ($null -ne $appliedResult) { "$appliedResult".Trim() } else { "" }

        if ([string]::IsNullOrWhiteSpace($applied)) {
            Write-Host "==> Applying incremental migration: $($file.Name)..."
            Get-Content -Raw -Encoding UTF8 $file.FullName | & docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database
            if ($LASTEXITCODE -ne 0) {
                throw "Migration failed for file: $($file.FullName)"
            }

            # 记录迁移版本
            $recordSql = "INSERT IGNORE INTO schema_migrations (version) VALUES ('$versionName');"
            & docker compose -f docker-compose.yml -f docker-compose.app.yml exec -T mysql mysql "-u$Username" "-p$Password" $Database -e "$recordSql"
            Write-Host "==> Migration $($file.Name) applied successfully."
        } else {
            Write-Host "==> Migration $($file.Name) already applied, skipping."
        }
    }
}

Write-Host "==> All database migrations completed successfully!"