param(
    [string]$BaseUrl = "http://localhost:3000",
    [string]$StudentUsername = "student_001",
    [string]$TeacherUsername = "teacher_demo",
    [string]$AdminUsername = "admin",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

Add-Type -AssemblyName System.Net.Http

$composeFiles = @("-f", "docker-compose.yml", "-f", "docker-compose.app.yml")
$runId = "TPREG_" + [DateTime]::UtcNow.ToString("yyyyMMddHHmmss") + "_" + ([Guid]::NewGuid().ToString("N").Substring(0, 6))
$courseCode = $runId
$courseName = "$runId course"
$tempUsername = ("tpreg" + [DateTime]::UtcNow.ToString("HHmmss") + (Get-Random -Minimum 100 -Maximum 999)).ToLowerInvariant()
$tempPassword = "Temp123456"
$updatedPassword = "Updated123456"
$courseId = $null
$tempUserId = $null
$baselineLogId = 0
$createdFiles = New-Object System.Collections.Generic.List[string]
$tempFiles = New-Object System.Collections.Generic.List[string]
$checks = 0
$runStartedAt = [DateTime]::UtcNow

function Get-ConfigValue([string]$Name, [string]$DefaultValue) {
    # 1. 优先读取 CI/CD 环境变量
    $envVal = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($envVal)) { return $envVal }

    # 2. 其次读取本地 .env 文件
    if (Test-Path -LiteralPath ".env") {
        $line = Get-Content -LiteralPath ".env" | Where-Object { $_ -match ('^' + [Regex]::Escape($Name) + '=') } | Select-Object -Last 1
        if ($line) { return ($line -split '=', 2)[1].Trim() }
    }

    # 3. 兜底默认值
    return $DefaultValue
}

$dbName = Get-ConfigValue "MYSQL_DATABASE" "teaching_platform"
$dbUsername = Get-ConfigValue "MYSQL_USER" "tp_dev"
$dbPassword = Get-ConfigValue "MYSQL_PASSWORD" "123456"

function Invoke-Db([string]$Sql) {
    $result = & docker compose @composeFiles exec -T mysql mysql `
        "--user=$dbUsername" "--password=$dbPassword" --default-character-set=utf8mb4 -N -B $dbName -e $Sql
    if ($LASTEXITCODE -ne 0) { throw "Database command failed: $Sql" }
    return (($result | ForEach-Object { [string]$_ }) -join "`n").Trim()
}

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}

function Invoke-NoRedirectRequest($Session, [string]$Uri, [string]$Method = "GET", [hashtable]$Body = $null) {
    $requestUri = [Uri]$Uri
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $false
    if ($Session) {
        foreach ($cookie in $Session.Cookies.GetCookies([Uri]$BaseUrl)) {
            $copy = New-Object System.Net.Cookie($cookie.Name, $cookie.Value, $cookie.Path, $requestUri.Host)
            $handler.CookieContainer.Add($requestUri, $copy)
        }
    }
    $client = New-Object System.Net.Http.HttpClient($handler)
    $request = New-Object System.Net.Http.HttpRequestMessage((New-Object System.Net.Http.HttpMethod($Method)), $requestUri)
    if ($Body) {
        $encodedBody = ($Body.GetEnumerator() | ForEach-Object {
            [Uri]::EscapeDataString([string]$_.Key) + "=" + [Uri]::EscapeDataString([string]$_.Value)
        }) -join "&"
        $request.Content = New-Object System.Net.Http.StringContent($encodedBody, [Text.Encoding]::UTF8, "application/x-www-form-urlencoded")
    }
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        if ($Session) {
            foreach ($cookie in $handler.CookieContainer.GetCookies($requestUri)) {
                $copy = New-Object System.Net.Cookie($cookie.Name, $cookie.Value, $cookie.Path, $requestUri.Host)
                $Session.Cookies.Add($requestUri, $copy)
            }
        }
        $location = if ($response.Headers.Location) {
            ([Uri]::new($requestUri, $response.Headers.Location)).AbsoluteUri
        } else { "" }
        return [pscustomobject]@{ Status = [int]$response.StatusCode; Location = $location }
    } finally {
        $request.Dispose()
        $client.Dispose()
        $handler.Dispose()
    }
}

function New-Session([string]$Username, [string]$LoginPassword = $Password) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $login = Invoke-NoRedirectRequest $session "$BaseUrl/login" "POST" @{
        username = $Username
        password = $LoginPassword
    }
    $status = $login.Status
    $location = $login.Location
    Assert-True ($status -in @(302, 303)) "Login failed for $Username (HTTP $status)."
    Assert-True ($location.StartsWith($BaseUrl + "/")) "Login for $Username escaped frontend origin: $location"
    $landingPage = Invoke-WebRequest -Uri "$BaseUrl/" -WebSession $session -UseBasicParsing
    Assert-True ($landingPage.StatusCode -eq 200) "Home page failed for $Username."
    return $session
}

function Assert-Page($Session, [string]$Path, [string]$Label) {
    $response = Invoke-WebRequest -Uri ($BaseUrl + $Path) -WebSession $Session -UseBasicParsing
    Assert-True ($response.StatusCode -eq 200) "$Label returned HTTP $($response.StatusCode): $Path"
    $body = [string]$response.Content
    Assert-True ($body -notmatch 'Whitelabel Error Page|Internal Server Error') "$Label rendered an error page: $Path"
}

function Get-NoRedirect($Session, [string]$Path) {
    return Invoke-NoRedirectRequest $Session ($BaseUrl + $Path)
}

function Assert-RedirectToLogin($Session, [string]$Path) {
    $response = Get-NoRedirect $Session $Path
    Assert-True ($response.Status -in @(302, 303)) "Expected access denial redirect for $Path, got HTTP $($response.Status)."
    Assert-True ($response.Location -match '/login(?:$|[?;])') "Expected $Path to redirect to login, got $($response.Location)."
}

function Post-Form($Session, [string]$Path, [hashtable]$Body) {
    $response = Invoke-NoRedirectRequest $Session ($BaseUrl + $Path) "POST" $Body
    $status = $response.Status
    $location = $response.Location
    Assert-True (($status -ge 200 -and $status -lt 300) -or ($status -in @(302, 303))) "POST $Path failed with HTTP $status."
    if ($location -match '^https?://' -and -not $location.StartsWith($BaseUrl + "/")) {
        throw "POST $Path redirected outside frontend origin: $location"
    }
    return [pscustomobject]@{ StatusCode = $status; Location = $location }
}

function Post-MultipartFile($Session, [string]$Path, [hashtable]$Fields, [string]$FieldName, [string]$FilePath, [string]$ContentType) {
    $uri = [Uri]($BaseUrl + $Path)
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $true
    foreach ($cookie in $Session.Cookies.GetCookies([Uri]$BaseUrl)) {
        $copy = New-Object System.Net.Cookie($cookie.Name, $cookie.Value, $cookie.Path, $uri.Host)
        $handler.CookieContainer.Add($uri, $copy)
    }
    $client = New-Object System.Net.Http.HttpClient($handler)
    $form = New-Object System.Net.Http.MultipartFormDataContent
    try {
        foreach ($entry in $Fields.GetEnumerator()) {
            $form.Add((New-Object System.Net.Http.StringContent([string]$entry.Value)), [string]$entry.Key)
        }
        $bytes = [IO.File]::ReadAllBytes($FilePath)
        $fileContent = New-Object System.Net.Http.ByteArrayContent -ArgumentList (,$bytes)
        $fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue($ContentType)
        $form.Add($fileContent, $FieldName, [IO.Path]::GetFileName($FilePath))
        $response = $client.PostAsync($uri, $form).GetAwaiter().GetResult()
        Assert-True ($response.IsSuccessStatusCode) "Multipart POST $Path failed with HTTP $([int]$response.StatusCode)."
    } finally {
        $form.Dispose()
        $client.Dispose()
        $handler.Dispose()
    }
}

function Remove-CreatedUpload([string]$StoredPath) {
    if ([string]::IsNullOrWhiteSpace($StoredPath)) { return }
    $portable = $StoredPath.Replace('\', '/')
    $marker = $portable.IndexOf('uploads/')
    if ($marker -ge 0) { $portable = $portable.Substring($marker + 8) }
    $uploadRoot = [IO.Path]::GetFullPath((Join-Path (Get-Location) "uploads"))
    $candidate = [IO.Path]::GetFullPath((Join-Path $uploadRoot $portable))
    if (-not $candidate.StartsWith($uploadRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove file outside uploads: $candidate"
    }
    if (Test-Path -LiteralPath $candidate) { Remove-Item -LiteralPath $candidate -Force }
}

try {
    Write-Host "[1/9] Checking containers and public routes..." -ForegroundColor Cyan
    $services = & docker compose @composeFiles ps --status running --services
    Assert-True (($services -contains "mysql") -and ($services -contains "backend") -and ($services -contains "frontend")) "All three services must be running."
    foreach ($path in @("/login", "/register", "/help", "/healthz")) {
        Assert-Page $null $path "Public page"
    }
    $baselineText = Invoke-Db "SELECT COALESCE(MAX(id),0) FROM operation_log;"
    $baselineLogId = if ($baselineText) { [long]$baselineText } else { 0 }

    Write-Host "[2/9] Logging in all roles and checking authorization..." -ForegroundColor Cyan
    $student = New-Session $StudentUsername
    $teacher = New-Session $TeacherUsername
    $admin = New-Session $AdminUsername
    Assert-RedirectToLogin $null "/admin/dashboard"
    Assert-RedirectToLogin $student "/teacher/course/manage"
    Assert-RedirectToLogin $student "/admin/dashboard"
    Assert-RedirectToLogin $teacher "/student/course/my"
    Assert-RedirectToLogin $teacher "/admin/dashboard"
    Assert-RedirectToLogin $admin "/student/course/my"

    Write-Host "[3/9] Testing administrator user and notification flows..." -ForegroundColor Cyan
    Post-Form $null "/register" @{ username = $tempUsername; password = $tempPassword; role = "student" } | Out-Null
    $tempUserId = [long](Invoke-Db "SELECT id FROM user WHERE username='$tempUsername';")
    Assert-True ($tempUserId -gt 0) "Temporary user was not created."
    Post-Form $admin "/admin/users/update" @{ userId = $tempUserId; name = "$runId user"; email = "$tempUsername@example.test"; role = "teacher" } | Out-Null
    Assert-True ((Invoke-Db "SELECT role FROM user WHERE id=$tempUserId;") -eq "teacher") "Administrator user update failed."
    Post-Form $admin "/admin/users/update" @{ userId = $tempUserId; name = "$runId user"; email = "$tempUsername@example.test"; role = "student" } | Out-Null
    Post-Form $admin "/admin/users/reset-password" @{ userId = $tempUserId; password = $updatedPassword } | Out-Null
    $tempUserSession = New-Session $tempUsername $updatedPassword
    Assert-Page $tempUserSession "/profile" "Reset-password login"

    Post-Form $admin "/admin/notifications/publish" @{ title = "$runId notice"; content = "container regression"; type = "system"; targetRole = "student" } | Out-Null
    $studentId = [long](Invoke-Db "SELECT id FROM user WHERE username='$StudentUsername';")
    $notificationId = [long](Invoke-Db "SELECT id FROM notification WHERE user_id=$studentId AND title='$runId notice' ORDER BY id DESC LIMIT 1;")
    Assert-True ($notificationId -gt 0) "Notification publish failed."
    Post-Form $student "/notifications/read" @{ notificationId = $notificationId } | Out-Null
    Assert-True ((Invoke-Db "SELECT is_read FROM notification WHERE id=$notificationId;") -eq "1") "Mark-notification-read failed."
    Post-Form $student "/notifications/read-all" @{} | Out-Null

    Write-Host "[4/9] Testing teacher course, class, task, and resource flows..." -ForegroundColor Cyan
    Post-Form $teacher "/teacher/course/create" @{
        courseName = $courseName; courseCode = $courseCode; credit = "2"; subjectCategory = "Regression"
        hours = "32"; allowJoin = "true"; status = "active"; description = "container regression course"
    } | Out-Null
    $courseId = [long](Invoke-Db "SELECT id FROM course WHERE code='$courseCode';")
    Assert-True ($courseId -gt 0) "Course create failed."
    Post-Form $teacher "/teacher/course/update" @{
        courseId = $courseId; courseName = "$courseName updated"; courseCode = $courseCode; credit = "3"
        subjectCategory = "RegressionUpdated"; hours = "48"; allowJoin = "true"; description = "updated"
    } | Out-Null
    Assert-True ((Invoke-Db "SELECT hours FROM course WHERE id=$courseId;") -eq "48") "Course update failed."

    Post-Form $teacher "/teacher/course/class/create" @{ courseId = $courseId; className = "$runId class"; maxCount = 40 } | Out-Null
    $classId = [long](Invoke-Db "SELECT id FROM course_class WHERE course_id=$courseId AND name='$runId class';")
    Assert-True ($classId -gt 0) "Class create failed."
    Post-Form $teacher "/teacher/course/class/update" @{ courseId = $courseId; classId = $classId; className = "$runId class updated"; maxCount = 45 } | Out-Null
    Assert-True ((Invoke-Db "SELECT max_count FROM course_class WHERE id=$classId;") -eq "45") "Class update failed."

    $taskSpecs = @(
        @{ Suffix = "homework"; Type = "homework"; Extra = @{} },
        @{ Suffix = "exam"; Type = "exam"; Extra = @{ examQuestions = "---QUESTION---`ntitle: Question 1`ntype: short`nscore: 100`n2+2?"; examAnswer = "4" } },
        @{ Suffix = "programming"; Type = "programming"; Extra = @{ allowedLanguage = "java"; testCases = "---CASE---`n`n---OUTPUT---`nHello World`n---WEIGHT---`n1"; timeLimitMs = 15000; memoryLimitMb = 128 } }
    )
    $taskIds = @{}
    foreach ($spec in $taskSpecs) {
        $body = @{ courseId = $courseId; title = "$runId $($spec.Suffix)"; taskType = $spec.Type; content = "regression"; fullScore = 100; status = "published" }
        foreach ($extra in $spec.Extra.GetEnumerator()) { $body[$extra.Key] = $extra.Value }
        Post-Form $teacher "/teacher/task/create" $body | Out-Null
        $taskIds[$spec.Suffix] = [long](Invoke-Db "SELECT id FROM task WHERE course_id=$courseId AND title='$runId $($spec.Suffix)';")
        Assert-True ($taskIds[$spec.Suffix] -gt 0) "Task create failed: $($spec.Suffix)."
    }
    Post-Form $teacher "/teacher/task/status" @{ taskId = $taskIds.homework; status = "draft" } | Out-Null
    Post-Form $teacher "/teacher/task/status" @{ taskId = $taskIds.homework; status = "published" } | Out-Null
    Assert-True ((Invoke-Db "SELECT status FROM task WHERE id=$($taskIds.homework);") -eq "published") "Task status update failed."

    $pdfFile = [IO.Path]::Combine([IO.Path]::GetTempPath(), "$runId.pdf")
    $videoFile = [IO.Path]::Combine([IO.Path]::GetTempPath(), "$runId.mp4")
    [IO.File]::WriteAllBytes($pdfFile, [Text.Encoding]::ASCII.GetBytes("%PDF-1.4`n% regression fixture`n"))
    [IO.File]::WriteAllBytes($videoFile, [byte[]](0, 0, 0, 24, 102, 116, 121, 112, 105, 115, 111, 109))
    $tempFiles.Add($pdfFile); $tempFiles.Add($videoFile)
    Post-MultipartFile $teacher "/teacher/resource/upload" @{ courseId = $courseId; title = "$runId pdf"; chapter = "Regression" } "file" $pdfFile "application/pdf"
    Post-MultipartFile $teacher "/teacher/resource/upload" @{ courseId = $courseId; title = "$runId video"; chapter = "Regression" } "file" $videoFile "video/mp4"
    $pdfResourceId = [long](Invoke-Db "SELECT id FROM resource WHERE course_id=$courseId AND title='$runId pdf';")
    $videoResourceId = [long](Invoke-Db "SELECT id FROM resource WHERE course_id=$courseId AND title='$runId video';")
    Assert-True (($pdfResourceId -gt 0) -and ($videoResourceId -gt 0)) "Resource upload failed."
    foreach ($stored in ((Invoke-Db "SELECT file_path FROM resource WHERE course_id=$courseId;") -split "`n")) {
        if ($stored) { $createdFiles.Add($stored) }
    }

    Write-Host "[5/9] Testing student enrollment, notes, submissions, exams, and judge..." -ForegroundColor Cyan
    Post-Form $student "/student/course/select" @{ courseId = $courseId } | Out-Null
    Assert-True ((Invoke-Db "SELECT COUNT(*) FROM course_enrollment WHERE student_id=$studentId AND course_id=$courseId;") -eq "1") "Course enrollment failed."
    Post-Form $student "/student/course/drop" @{ courseId = $courseId } | Out-Null
    Assert-True ((Invoke-Db "SELECT COUNT(*) FROM course_enrollment WHERE student_id=$studentId AND course_id=$courseId;") -eq "0") "Course drop failed."
    Post-Form $student "/student/course/select" @{ courseId = $courseId } | Out-Null

    Post-Form $student "/student/notes/save" @{ courseId = $courseId; title = "$runId note"; content = "first" } | Out-Null
    $noteId = [long](Invoke-Db "SELECT id FROM study_note WHERE student_id=$studentId AND title='$runId note';")
    Assert-True ($noteId -gt 0) "Study note create failed."
    Post-Form $student "/student/notes/save" @{ noteId = $noteId; courseId = $courseId; title = "$runId note"; content = "updated" } | Out-Null
    Assert-True ((Invoke-Db "SELECT content FROM study_note WHERE id=$noteId;") -eq "updated") "Study note update failed."

    Post-Form $student "/student/task/submit" @{ taskId = $taskIds.homework; content = "$runId homework answer" } | Out-Null
    $homeworkSubmissionId = [long](Invoke-Db "SELECT id FROM submission WHERE task_id=$($taskIds.homework) AND student_id=$studentId;")
    Assert-True ($homeworkSubmissionId -gt 0) "Homework submission failed."
    Post-Form $teacher "/teacher/task/grade" @{ submissionId = $homeworkSubmissionId; score = 95; comment = "$runId graded" } | Out-Null
    Assert-True ((Invoke-Db "SELECT CONCAT(status,':',CAST(score AS CHAR)) FROM submission WHERE id=$homeworkSubmissionId;") -match '^graded:95') "Homework grading failed."

    Post-Form $student "/student/exam/begin" @{ taskId = $taskIds.exam } | Out-Null
    $saved = Invoke-RestMethod -Uri "$BaseUrl/student/exam/save" -Method Post -Body @{ taskId = $taskIds.exam; content = '{"answers":{"1":"4"},"attachments":{}}' } -WebSession $student
    Assert-True (($saved.code -eq 200) -and $saved.saved) "Exam save failed."
    Post-Form $student "/student/exam/submit" @{ taskId = $taskIds.exam; content = '{"answers":{"1":"4"},"attachments":{}}' } | Out-Null
    Assert-True ((Invoke-Db "SELECT status FROM exam_record WHERE task_id=$($taskIds.exam) AND student_id=$studentId;") -eq "SUBMITTED") "Exam submit failed."
    Assert-True ((Invoke-Db "SELECT COUNT(*) FROM submission WHERE task_id=$($taskIds.exam) AND student_id=$studentId;") -eq "1") "Exam submission projection failed."

    $judgeBody = @{ taskId = $taskIds.programming; language = "java"; code = 'public class Main { public static void main(String[] args) { System.out.println("Hello World"); } }' } | ConvertTo-Json -Depth 6
    $judge = Invoke-RestMethod -Uri "$BaseUrl/api/v2/judge/submit" -Method Post -ContentType "application/json" -Body $judgeBody -WebSession $student -TimeoutSec 60
    Assert-True (($judge.code -eq 200) -and ($judge.data.status -eq "AC")) "Programming judge failed: $($judge | ConvertTo-Json -Depth 6 -Compress)"

    $download = Invoke-WebRequest -Uri "$BaseUrl/student/resource/download/$pdfResourceId" -WebSession $student -UseBasicParsing
    Assert-True (($download.StatusCode -eq 200) -and ($download.RawContentLength -gt 0)) "PDF resource download failed."
    Assert-Page $student "/student/resource/video/$videoResourceId" "Video page"
    $stream = Invoke-WebRequest -Uri "$BaseUrl/student/resource/stream/$videoResourceId" -WebSession $student -UseBasicParsing
    Assert-True (($stream.StatusCode -eq 200) -and ($stream.RawContentLength -gt 0)) "Video stream failed."
    $progressBody = @{ resourceId = $videoResourceId; currentTime = 5; duration = 10 } | ConvertTo-Json
    $progress = Invoke-RestMethod -Uri "$BaseUrl/student/resource/progress" -Method Post -ContentType "application/json" -Body $progressBody -WebSession $student
    Assert-True ($progress.code -eq 200) "Video progress update failed."

    Write-Host "[6/9] Testing discussion and score export flows..." -ForegroundColor Cyan
    Post-Form $student "/discussion/post" @{ courseId = $courseId; title = "$runId post"; content = "question"; postType = "question"; targetRole = "teacher" } | Out-Null
    $postId = [long](Invoke-Db "SELECT id FROM discussion_post WHERE course_id=$courseId AND title='$runId post';")
    Assert-True ($postId -gt 0) "Discussion post create failed."
    Post-Form $teacher "/discussion/reply" @{ postId = $postId; content = "$runId reply"; assistantReply = "true" } | Out-Null
    Assert-True ((Invoke-Db "SELECT COUNT(*) FROM discussion_reply WHERE post_id=$postId AND content='$runId reply';") -eq "1") "Discussion reply failed."
    $scoreCsv = Invoke-WebRequest -Uri "$BaseUrl/teacher/score/export?courseId=$courseId" -WebSession $teacher -UseBasicParsing
    Assert-True ($scoreCsv.StatusCode -eq 200) "Score export failed."
    Assert-True ([string]$scoreCsv.Headers.'Content-Disposition' -match 'score-report') "Score export filename is missing."

    Write-Host "[7/9] Rendering all role pages and dynamic routes..." -ForegroundColor Cyan
    $common = @("/", "/home", "/profile", "/notifications", "/resource-square", "/help")
    foreach ($path in $common) { Assert-Page $student $path "Student common page" }
    foreach ($path in @(
        "/student/course/my", "/student/course/selection", "/student/classes", "/student/tasks",
        "/student/tasks?type=homework", "/student/tasks?type=exam", "/student/tasks?type=programming",
        "/student/notes", "/student/scores", "/student/logs", "/student/lab", "/student/ai",
        "/student/course/detail/${courseId}?tab=home", "/student/course/detail/${courseId}?tab=info",
        "/student/course/detail/${courseId}?tab=homework", "/student/course/detail/${courseId}?tab=exam",
        "/student/course/detail/${courseId}?tab=lab", "/student/course/detail/${courseId}?tab=resources",
        "/student/course/detail/${courseId}?tab=progress", "/student/course/detail/${courseId}?tab=discussion",
        "/student/task/detail?taskId=$($taskIds.homework)", "/student/exam/start?taskId=$($taskIds.exam)",
        "/discussion/post/$postId"
    )) { Assert-Page $student $path "Student page" }

    foreach ($path in $common) { Assert-Page $teacher $path "Teacher common page" }
    foreach ($path in @(
        "/teacher/course/manage", "/teacher/course/create", "/teacher/course/edit/$courseId",
        "/teacher/task/manage", "/teacher/task/create?courseId=$courseId", "/teacher/task/detail/$($taskIds.homework)",
        "/teacher/task/grade/$homeworkSubmissionId", "/teacher/course/class/$courseId", "/teacher/resource/manage/$courseId",
        "/teacher/score/statistics", "/teacher/lab", "/teacher/ai", "/discussion/teacher/course/$courseId", "/discussion/post/$postId"
    )) { Assert-Page $teacher $path "Teacher page" }

    foreach ($path in @("/", "/profile", "/notifications", "/resource-square", "/help", "/admin/dashboard", "/admin/users", "/admin/notifications", "/admin/logs")) {
        Assert-Page $admin $path "Administrator page"
    }

    Write-Host "[8/9] Testing delete and retract operations..." -ForegroundColor Cyan
    Post-Form $student "/student/notes/delete" @{ noteId = $noteId } | Out-Null
    Assert-True ((Invoke-Db "SELECT COUNT(*) FROM study_note WHERE id=$noteId;") -eq "0") "Study note delete failed."
    $deleteClass = Get-NoRedirect $teacher "/teacher/course/class/delete?courseId=${courseId}&classId=$classId"
    Assert-True ($deleteClass.Status -in @(302, 303)) "Class delete did not redirect."
    Assert-True ((Invoke-Db "SELECT COUNT(*) FROM course_class WHERE id=$classId;") -eq "0") "Class delete failed."
    $deleteTask = Get-NoRedirect $teacher "/teacher/task/delete?taskId=$($taskIds.homework)"
    Assert-True ($deleteTask.Status -in @(302, 303)) "Task retract did not redirect."
    Assert-True ((Invoke-Db "SELECT status FROM task WHERE id=$($taskIds.homework);") -eq "retracted") "Task retract failed."
    Post-Form $admin "/admin/users/delete" @{ userId = $tempUserId } | Out-Null
    Assert-True ((Invoke-Db "SELECT COUNT(*) FROM user WHERE id=$tempUserId;") -eq "0") "Administrator user delete failed."
    $tempUserId = $null

    Write-Host "[9/9] Checking application logs..." -ForegroundColor Cyan
    $since = $runStartedAt.ToString("o")
    $errors = & docker compose @composeFiles logs --since $since backend 2>&1 | Select-String -Pattern 'ERROR|Exception|Whitelabel' -CaseSensitive:$false
    Assert-True (-not $errors) ("Backend logged errors during regression:`n" + ($errors -join "`n"))
    Write-Host "Business regression passed: $checks assertions." -ForegroundColor Green
} finally {
    Write-Host "Cleaning temporary regression data..." -ForegroundColor DarkGray
    try {
        if ($courseId) { Invoke-Db "DELETE FROM course WHERE id=$courseId;" | Out-Null }
        Invoke-Db "DELETE FROM notification WHERE title LIKE '$runId%';" | Out-Null
        if ($tempUserId) { Invoke-Db "DELETE FROM user WHERE id=$tempUserId;" | Out-Null }
        Invoke-Db "DELETE FROM user WHERE username='$tempUsername';" | Out-Null
        if ($baselineLogId -ge 0) { Invoke-Db "DELETE FROM operation_log WHERE id>$baselineLogId;" | Out-Null }
    } catch {
        Write-Warning "Database cleanup was incomplete: $($_.Exception.Message)"
    }
    foreach ($storedPath in $createdFiles) {
        try { Remove-CreatedUpload $storedPath } catch { Write-Warning $_.Exception.Message }
    }
    foreach ($tempPath in $tempFiles) {
        if (Test-Path -LiteralPath $tempPath) { Remove-Item -LiteralPath $tempPath -Force }
    }
}
