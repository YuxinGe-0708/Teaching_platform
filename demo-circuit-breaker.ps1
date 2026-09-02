Write-Host "[1/6] Login..."
$loginBody = '{"username":"24373116","password":"123456"}'
$loginResponse = Invoke-WebRequest -Uri "http://localhost:8082/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -UseBasicParsing
$token = ($loginResponse.Content | ConvertFrom-Json).data.token
Write-Host "Login OK"
Write-Host ""

Write-Host "[2/6] Normal call (before circuit breaker)..."
$headers = @{Authorization = "Bearer $token"}
$response = Invoke-WebRequest -Uri "http://localhost:8082/internal/users/course/1" -Method GET -Headers $headers -UseBasicParsing
Write-Host "Response: $($response.Content)"
Write-Host ""

Write-Host "[3/6] Stop learning-service..."
docker stop teaching_platform-learning-service-1
Write-Host "learning-service stopped"
Write-Host ""

Write-Host "[4/6] Call 5 times to trigger circuit breaker..."
for ($i=1; $i -le 5; $i++) {
    Write-Host "Call $i..."
    $resp = Invoke-WebRequest -Uri "http://localhost:8082/internal/users/course/1" -Method GET -Headers $headers -UseBasicParsing 2>$null
    if ($resp) {
        Write-Host "Response: $($resp.Content)"
    }
    Start-Sleep -Seconds 1
}
Write-Host "Circuit breaker triggered"
Write-Host ""

Write-Host "[5/6] Check circuit breaker logs..."
$logs = docker logs teaching_platform-user-service-1 --tail 20 2>&1
Write-Host $logs
Write-Host ""

Write-Host "[6/6] Restore learning-service..."
docker start teaching_platform-learning-service-1
Write-Host "learning-service restored"
Write-Host ""

Write-Host "Demo completed!"