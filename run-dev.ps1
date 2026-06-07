param(
    [string]$DbUsername = "root",
    [string]$DbPassword = "",
    [string]$DbName = "teaching_platform",
    [int]$Port = 8080,
    [switch]$SeedTestData,
    [switch]$SkipDbCreate,
    [switch]$UseQwen,
    [string]$QwenApiKey = $env:QWEN_API_KEY,
    [string]$QwenTextModel = "qwen-plus",
    [string]$QwenVisionModel = "qwen3-vl-plus",
    [string]$Judge0ApiUrl = "https://ce.judge0.com",
    [string]$Judge0ApiKey = $env:JUDGE0_API_KEY,
    [string]$Judge0ApiHost = $env:JUDGE0_API_HOST,
    [int]$Judge0TimeoutMs = 15000
)

$ErrorActionPreference = "Stop"

function Read-PlainPassword {
    if ($DbPassword -and $DbPassword.Trim().Length -gt 0) {
        return $DbPassword
    }
    $secure = Read-Host "Input MySQL password" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Read-SecretText([string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

Write-Host "== Teaching Platform dev runner ==" -ForegroundColor Cyan

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    throw "java was not found. Please install JDK 8 and make sure java is in PATH."
}

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    throw "mvn was not found. Please install Maven and make sure mvn is in PATH."
}

$plainPassword = Read-PlainPassword

if (-not $SkipDbCreate) {
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysql) {
        Write-Host "Ensuring database exists: $DbName" -ForegroundColor Cyan
        $createDbSql = "CREATE DATABASE IF NOT EXISTS ``$DbName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        & mysql -u $DbUsername "-p$plainPassword" -e $createDbSql
    } else {
        Write-Host "mysql command was not found. Skip database creation. Create database manually if needed: $DbName" -ForegroundColor Yellow
    }
}

$env:DB_USERNAME = $DbUsername
$env:DB_PASSWORD = $plainPassword
$env:DB_URL = "jdbc:mysql://localhost:3306/$DbName" + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8"
$env:SERVER_PORT = "$Port"
$env:APP_SEED_TEST_DATA = if ($SeedTestData) { "true" } else { "false" }

if ($UseQwen) {
    if (-not $QwenApiKey -or $QwenApiKey.Trim().Length -eq 0) {
        $QwenApiKey = Read-SecretText "Input Qwen/DashScope API key"
    }
    $env:AI_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    $env:AI_API_KEY = $QwenApiKey
    $env:AI_MODEL = $QwenTextModel
    $env:AI_VISION_MODEL = $QwenVisionModel
}

$env:JUDGE0_API_URL = $Judge0ApiUrl
$env:JUDGE0_API_KEY = if ($Judge0ApiKey) { $Judge0ApiKey } else { "" }
$env:JUDGE0_API_HOST = if ($Judge0ApiHost) { $Judge0ApiHost } else { "" }
$env:JUDGE0_TIMEOUT_MS = "$Judge0TimeoutMs"

Write-Host "DB username: $DbUsername" -ForegroundColor Gray
Write-Host "DB name: $DbName" -ForegroundColor Gray
Write-Host "Server port: $Port" -ForegroundColor Gray
Write-Host "Seed test data: $env:APP_SEED_TEST_DATA" -ForegroundColor Gray
Write-Host "AI provider: $(if ($UseQwen) { 'Qwen/DashScope' } else { 'default application config' })" -ForegroundColor Gray
Write-Host "Judge0 URL: $env:JUDGE0_API_URL" -ForegroundColor Gray
Write-Host "Open after startup: http://localhost:$Port/login" -ForegroundColor Green

Write-Host "Starting Spring Boot..." -ForegroundColor Cyan
mvn spring-boot:run
