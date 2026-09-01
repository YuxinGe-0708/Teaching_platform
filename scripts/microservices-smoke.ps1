param([string]$BaseUrl = "http://localhost:3000")
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Assert-Ok([string]$Uri) {
    $r = Invoke-WebRequest -Uri $Uri -UseBasicParsing
    if ($r.StatusCode -lt 200 -or $r.StatusCode -ge 300) { throw "$Uri returned HTTP $($r.StatusCode)" }
    return $r
}

$health = Assert-Ok "$BaseUrl/healthz"
if ([string]$health.Content -notmatch "ok") { throw "Gateway health response is not ok" }
foreach ($path in @("/login", "/register", "/help")) { Assert-Ok "$BaseUrl$path" | Out-Null }
foreach ($port in 8082,8083,8084) {
    $serviceHealth = Invoke-RestMethod -Uri "http://localhost:$port/actuator/health/readiness"
    if ($serviceHealth.status -ne "UP") { throw "Service $port is not healthy" }
}
Write-Host "Microservice gateway and all service health checks passed."
