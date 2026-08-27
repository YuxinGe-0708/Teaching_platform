param(
    [string]$BaseUrl = "http://localhost:3000",
    [string]$BackendUrl = "http://localhost:8081",
    [string]$Username = "admin",
    [string]$Password = "123456",
    [switch]$SkipFrontendHealth
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

function Invoke-LoginNoRedirect([string]$Uri, [string]$LoginUsername, [string]$LoginPassword, $Session) {
    $requestUri = [Uri]$Uri
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $false
    $client = New-Object System.Net.Http.HttpClient($handler)
    $encoded = "username=$([Uri]::EscapeDataString($LoginUsername))&password=$([Uri]::EscapeDataString($LoginPassword))"
    $content = New-Object System.Net.Http.StringContent($encoded, [Text.Encoding]::UTF8, "application/x-www-form-urlencoded")
    try {
        $response = $client.PostAsync($requestUri, $content).GetAwaiter().GetResult()
        foreach ($cookie in $handler.CookieContainer.GetCookies($requestUri)) {
            $copy = New-Object System.Net.Cookie($cookie.Name, $cookie.Value, $cookie.Path, $requestUri.Host)
            $Session.Cookies.Add($requestUri, $copy)
        }
        $location = if ($response.Headers.Location) {
            ([Uri]::new($requestUri, $response.Headers.Location)).AbsoluteUri
        } else { "" }
        return [pscustomobject]@{ Status = [int]$response.StatusCode; Location = $location }
    } finally {
        $content.Dispose()
        $client.Dispose()
        $handler.Dispose()
    }
}

function Assert-HttpOk([string]$Uri) {
    $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing
    if ($response.StatusCode -ne 200) {
        throw "$Uri returned HTTP $($response.StatusCode)"
    }
    return $response
}

if (-not $SkipFrontendHealth) {
    Write-Host "Checking frontend health..." -ForegroundColor Cyan
    $health = Assert-HttpOk "$BaseUrl/healthz"
    $healthContent = if ($health.Content -is [byte[]]) {
        [System.Text.Encoding]::UTF8.GetString($health.Content)
    } else {
        [string]$health.Content
    }
    if ($healthContent.Trim() -ne "ok") {
        throw "Frontend health response was not 'ok'."
    }
}

Write-Host "Checking original page routes through frontend..." -ForegroundColor Cyan
Assert-HttpOk "$BaseUrl/login" | Out-Null
Assert-HttpOk "$BaseUrl/register" | Out-Null
Assert-HttpOk "$BaseUrl/help" | Out-Null

Write-Host "Checking backend route..." -ForegroundColor Cyan
Assert-HttpOk "$BackendUrl/login" | Out-Null

Write-Host "Checking login/session flow..." -ForegroundColor Cyan
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-LoginNoRedirect "$BaseUrl/login" $Username $Password $session
$loginStatus = $login.Status
$loginLocation = $login.Location
if ($loginStatus -notin @(302, 303)) {
    throw "Login did not redirect; HTTP $loginStatus"
}
if ($loginLocation -and -not $loginLocation.StartsWith($BaseUrl + "/")) {
    throw "Login redirected outside frontend origin: $loginLocation"
}

$homeResponse = Invoke-WebRequest -Uri "$BaseUrl/" -WebSession $session -UseBasicParsing
if ($homeResponse.StatusCode -ne 200 -or $homeResponse.Content -notmatch "Teaching\s*Platform") {
    throw "Authenticated home page did not render correctly."
}

Write-Host "Container smoke test passed." -ForegroundColor Green
