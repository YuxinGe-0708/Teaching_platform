param(
    [string]$BaseUrl = "http://localhost:3000",
    [string]$Username = "student_001",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

Add-Type -AssemblyName System.Net.Http

function New-AuthenticatedSession([string]$LoginUsername, [string]$LoginPassword) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $uri = [Uri]"$BaseUrl/login"
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $false
    $client = New-Object System.Net.Http.HttpClient($handler)
    $encoded = "username=$([Uri]::EscapeDataString($LoginUsername))&password=$([Uri]::EscapeDataString($LoginPassword))"
    $content = New-Object System.Net.Http.StringContent($encoded, [Text.Encoding]::UTF8, "application/x-www-form-urlencoded")
    try {
        $response = $client.PostAsync($uri, $content).GetAwaiter().GetResult()
        if ([int]$response.StatusCode -notin @(302, 303)) {
            throw "Login failed with HTTP $([int]$response.StatusCode)."
        }
        foreach ($cookie in $handler.CookieContainer.GetCookies($uri)) {
            $copy = New-Object System.Net.Cookie($cookie.Name, $cookie.Value, $cookie.Path, $uri.Host)
            $session.Cookies.Add($uri, $copy)
        }
        return $session
    } finally {
        $content.Dispose()
        $client.Dispose()
        $handler.Dispose()
    }
}

$session = New-AuthenticatedSession $Username $Password

$chatBody = @{
    message = "Reply with OK only."
    courseName = "Container regression"
    courseId = "0"
} | ConvertTo-Json
$chat = Invoke-RestMethod -Uri "$BaseUrl/api/v2/ai/chat" -Method Post -ContentType "application/json" `
    -Body $chatBody -WebSession $session -TimeoutSec 90
$chatReply = [string]$chat.data.reply
if ($chat.code -ne 200 -or [string]::IsNullOrWhiteSpace($chatReply) -or $chatReply -match "error|invalid") {
    throw "Qwen chat failed: $chatReply"
}

# A valid 32x32 PNG avoids the vision model's minimum-dimension rejection.
$png = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAACVSURBVFhH7c47AsAgCANQ7n/pdumEIuETupgRIT55fo7owXQuYAGILCNqtr9NIsyfphDHXyYQ7g9sBNTORMDNLESolYEIN3YjUm2diHRTF6LU0oEoN1QRtesvFUT+UiWLyF0ZySDiF06iiNg2mAgC3wwGRWBbySAIf6MYD3F+bcoJYb80x0Lsp6TsEOuEHI0YB+hcwAtlK6Yt3yRmrQAAAABJRU5ErkJggg=="
$visionBody = @{
    image = "data:image/png;base64,$png"
    courseName = "Container regression"
    resourceTitle = "Diagonal line test image"
} | ConvertTo-Json
$vision = Invoke-RestMethod -Uri "$BaseUrl/api/v2/ai/explain-image" -Method Post -ContentType "application/json" `
    -Body $visionBody -WebSession $session -TimeoutSec 120
$visionReply = [string]$vision.data.reply
if ($vision.code -ne 200 -or [string]::IsNullOrWhiteSpace($visionReply) -or $visionReply -match "error|invalid") {
    throw "Qwen vision failed: $visionReply"
}

Write-Host "Qwen chat and vision smoke tests passed." -ForegroundColor Green
