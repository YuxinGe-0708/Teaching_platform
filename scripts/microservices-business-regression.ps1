param(
    [string]$BaseUrl = "http://localhost:3000",
    [string]$UserServiceUrl = "http://localhost:8082",
    [string]$LearningServiceUrl = "http://localhost:8083",
    [string]$AssessmentServiceUrl = "http://localhost:8084"
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$internalKey = if ($env:INTERNAL_API_KEY) { $env:INTERNAL_API_KEY } else { "dev-internal-key" }
$headers = @{ "X-Internal-Api-Key" = $internalKey }
$UserServiceUrl = $UserServiceUrl.TrimEnd('/')
$LearningServiceUrl = $LearningServiceUrl.TrimEnd('/')
$AssessmentServiceUrl = $AssessmentServiceUrl.TrimEnd('/')
$checks = 0

function Assert-True([bool]$condition, [string]$message) { if (-not $condition) { throw $message }; $script:checks++ }
function Get-Json([string]$uri, [hashtable]$h = $null) {
    $r = Invoke-RestMethod -Uri $uri -Headers $h -Method Get
    Assert-True ($r.code -ge 200 -and $r.code -lt 300) "GET failed: $uri"
    return $r.data
}
function Send-Json([string]$uri, [string]$method, $body, [hashtable]$h = $null) {
    $r = Invoke-RestMethod -Uri $uri -Headers $h -Method $method -ContentType "application/json" -Body ($body | ConvertTo-Json -Depth 10)
    Assert-True ($r.code -ge 200 -and $r.code -lt 300) "$method failed: $uri ($($r.message))"
    return $r.data
}
function Send-Envelope([string]$uri, [string]$method, $body, [hashtable]$h = $null) {
    return Invoke-RestMethod -Uri $uri -Headers $h -Method $method -ContentType "application/json" -Body ($body | ConvertTo-Json -Depth 10)
}
function Send-SessionJson([string]$uri, $body) {
    $r = Invoke-RestMethod -Uri $uri -WebSession $script:pageSession -Method Post -ContentType "application/json" -Body ($body | ConvertTo-Json -Depth 10)
    Assert-True ($r.code -ge 200 -and $r.code -lt 300) "Session POST failed: $uri ($($r.message))"
    return $r.data
}
function Invoke-WebRequestCompat([hashtable]$parameters) {
    if ((Get-Command Invoke-WebRequest).Parameters.ContainsKey("SkipHttpErrorCheck")) {
        $parameters["SkipHttpErrorCheck"] = $true
    }
    try {
        return Invoke-WebRequest @parameters
    } catch [System.Net.WebException] {
        if ($_.Exception.Response -ne $null) {
            $response = $_.Exception.Response
            return [pscustomobject]@{
                StatusCode = [int]$response.StatusCode
                Content = ""
            }
        }
        throw
    }
}
function Get-StatusWithoutRedirect([string]$uri) {
    $response = Invoke-WebRequestCompat @{
        Uri = $uri
        MaximumRedirection = 10
        UseBasicParsing = $true
    }
    return [int]$response.StatusCode
}

Write-Host "[1/8] User service: registration and login"
$suffix = [DateTime]::UtcNow.ToString("HHmmssfff")
$username = "ci_$suffix"
$registered = Send-Json "$BaseUrl/api/auth/register" "POST" @{ username=$username; password="Temp123456"; role="student"; name="CI Student" }
Assert-True ($registered.username -eq $username) "Registration response mismatch"
$duplicate = Send-Envelope "$BaseUrl/api/auth/register" "POST" @{ username=$username; password="Temp123456"; role="student"; name="Duplicate" }
Assert-True ($duplicate.code -eq 400) "Duplicate registration exception flow was not rejected"
$badLogin = Send-Envelope "$BaseUrl/api/auth/login" "POST" @{ username=$username; password="wrong-password" }
Assert-True ($badLogin.code -eq 401) "Invalid password exception flow was not rejected"
$login = Send-Json "$BaseUrl/api/auth/login" "POST" @{ username=$username; password="Temp123456" }
Assert-True (-not [string]::IsNullOrWhiteSpace($login.token)) "Login token missing"
$pageSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$pageLogin = Invoke-WebRequestCompat @{
    Uri = "$BaseUrl/login"
    Method = "Post"
    Body = @{ username=$username; password="Temp123456" }
    WebSession = $pageSession
    UseBasicParsing = $true
}
Assert-True ($pageLogin.StatusCode -in @(200,302,303)) "BFF login did not return a success response"
$sessionCookies = $pageSession.Cookies.GetCookies([uri]$BaseUrl)
Assert-True ($sessionCookies['JSESSIONID'] -ne $null) "BFF login did not establish a session"
Get-Json "$UserServiceUrl/internal/users/$($registered.id)" $headers | Out-Null
Get-Json "$UserServiceUrl/internal/users?role=student" $headers | Out-Null
Get-Json "$UserServiceUrl/internal/notifications/user/$($registered.id)" $headers | Out-Null

Write-Host "[2/8] Learning service: courses, enrollment and classes"
$courses = Get-Json "$LearningServiceUrl/internal/courses/active" $headers
Assert-True ($courses.Count -gt 0) "No active learning courses"
$course = $courses[0]
Get-Json "$LearningServiceUrl/internal/courses/$($course.id)" $headers | Out-Null
$enrollment = Send-Json "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=$($course.id)" "POST" @{}
Assert-True ($null -ne $enrollment) "Enrollment failed"
$duplicateEnrollment = Send-Json "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=$($course.id)" "POST" @{}
Assert-True ($duplicateEnrollment.id -eq $enrollment.id) "Duplicate enrollment alternate flow did not return the existing row"
$null = Send-Json "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=$($course.id)" "DELETE" @{}
$secondDrop = Send-Envelope "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=$($course.id)" "DELETE" @{}
Assert-True ($secondDrop.code -eq 400) "Dropping a non-enrolled course exception flow was not rejected"
$missingCourse = Send-Envelope "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=99999999" "POST" @{}
Assert-True ($missingCourse.code -eq 400) "Missing course exception flow was not rejected"
$enrollment = Send-Json "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=$($course.id)" "POST" @{}
Assert-True ($null -ne $enrollment) "Re-enrollment after drop failed"
Get-Json "$LearningServiceUrl/api/enrollments/student/$($registered.id)" | Out-Null
Get-Json "$LearningServiceUrl/internal/enrollments/check?studentId=$($registered.id)&courseId=$($course.id)" $headers | Out-Null
Get-Json "$LearningServiceUrl/internal/classes/course/$($course.id)" $headers | Out-Null

Write-Host "[3/8] Learning service: resources, progress and notes"
$resource = Send-Json "$LearningServiceUrl/internal/bff/resources" "POST" @{ courseId=$course.id; title="CI resource $suffix"; filePath="uploads/ci/test.txt"; type="text"; chapter="CI"; fileSize=2 } $headers
Assert-True ($resource.id -gt 0) "Resource creation failed"
$null = Send-Json "$LearningServiceUrl/api/resource-progress?studentId=$($registered.id)&resourceId=$($resource.id)&progress=50&lastPosition=5&duration=10" "POST" @{}
$progress = Get-Json "$LearningServiceUrl/api/resource-progress?studentId=$($registered.id)&resourceId=$($resource.id)"
Assert-True ([double]$progress.progress -eq 50) "Resource progress was not persisted"
$missingResource = Invoke-RestMethod -Uri "$LearningServiceUrl/api/resources/99999999" -Method Get
Assert-True ($missingResource.code -eq 404) "Missing resource exception flow was not returned"
$note = Send-Json "$LearningServiceUrl/internal/bff/notes" "POST" @{ studentId=$registered.id; courseId=$course.id; resourceId=$resource.id; title="CI note $suffix"; content="microservice note" } $headers
Assert-True ($note.id -gt 0) "Study note creation failed"
Get-Json "$LearningServiceUrl/internal/bff/notes/student/$($registered.id)" $headers | Out-Null

Write-Host "[4/8] Learning service: discussions"
Get-Json "$LearningServiceUrl/api/discussions/posts/course/$($course.id)" | Out-Null
$post = Send-Json "$LearningServiceUrl/api/discussions/posts?courseId=$($course.id)&userId=$($registered.id)&title=CI%20question&content=CI%20discussion" "POST" @{}
Assert-True ($null -ne $post) "Discussion post failed"
Get-Json "$LearningServiceUrl/api/discussions/replies/$($post.id)" | Out-Null

Write-Host "[5/8] Learning service: AI endpoints"
$aiReply = Send-SessionJson "$BaseUrl/api/v2/ai/chat" @{ sessionId="ci-$suffix"; courseName=$course.name; message="Explain microservice service boundaries." }
Assert-True (-not [string]::IsNullOrWhiteSpace($aiReply.reply)) "AI chat returned no response"
$emptyAi = Invoke-RestMethod -Uri "$BaseUrl/api/v2/ai/chat" -WebSession $pageSession -Method Post -ContentType "application/json" -Body '{"message":" "}'
Assert-True ($emptyAi.code -eq 400) "Empty AI message exception flow was not rejected"
$mindMap = Send-Json "$LearningServiceUrl/api/v2/ai/mind-map" "POST" @{ courseName=$course.name; title="CI note"; text="gateway BFF user-service learning-service assessment-service" }
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$mindMap)) "AI mind-map returned no response"

Write-Host "[6/8] Assessment service: task list and scores"
$tasks = Get-Json "$AssessmentServiceUrl/internal/tasks?courseId=$($course.id)" $headers
Get-Json "$AssessmentServiceUrl/internal/tasks/student/$($registered.id)" $headers | Out-Null
Get-Json "$AssessmentServiceUrl/internal/scores/student/$($registered.id)" $headers | Out-Null
$missingTaskSubmission = Send-Envelope "$AssessmentServiceUrl/internal/submissions?taskId=99999999&studentId=$($registered.id)" "POST" @{} $headers
Assert-True ($missingTaskSubmission.code -eq 404) "Missing assessment task exception flow was not rejected"

Write-Host "[7/8] Assessment service: submission and exam routes"
if ($tasks.Count -gt 0) {
    $task = @($tasks | Where-Object { $_.type -eq "homework" })[0]
    $submission = Send-Json "$AssessmentServiceUrl/internal/submissions?taskId=$($task.id)&studentId=$($registered.id)&content=CI%20answer" "POST" $null $headers
    Assert-True ($null -ne $submission) "Submission failed"
    $graded = Send-Json "$AssessmentServiceUrl/internal/submissions/$($submission.id)/grade?score=88&feedback=CI%20graded" "POST" $null $headers
    Assert-True ($graded.status -eq "graded") "Homework grading failed"
    Get-Json "$AssessmentServiceUrl/internal/submissions/student/$($registered.id)" $headers | Out-Null
    Get-Json "$AssessmentServiceUrl/internal/scores/student/$($registered.id)/course/$($course.id)" $headers | Out-Null
}

$examCourse = @($courses | Where-Object { $_.code -eq "MS301" })[0]
Assert-True ($null -ne $examCourse) "Exam fixture course is missing"
$null = Send-Json "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=$($examCourse.id)" "POST" @{}
$examTasks = Get-Json "$AssessmentServiceUrl/internal/tasks?courseId=$($examCourse.id)" $headers
$examTask = @($examTasks | Where-Object { $_.type -eq "exam" })[0]
Assert-True ($null -ne $examTask) "Exam fixture task is missing"
$exam = Send-Json "$AssessmentServiceUrl/internal/exams/$($examTask.id)/begin?studentId=$($registered.id)" "POST" $null $headers
Assert-True ($exam.status -eq "IN_PROGRESS") "Exam did not begin"
$exam = Send-Json "$AssessmentServiceUrl/internal/exams/$($examTask.id)/progress?studentId=$($registered.id)&content=CI%20answer" "PUT" $null $headers
$exam = Send-Json "$AssessmentServiceUrl/internal/exams/$($examTask.id)/submit?studentId=$($registered.id)&content=CI%20answer" "POST" $null $headers
Assert-True ($exam.status -eq "SUBMITTED") "Exam submission failed"

$programmingCourse = @($courses | Where-Object { $_.code -eq "MS202" })[0]
Assert-True ($null -ne $programmingCourse) "Programming fixture course is missing"
$null = Send-Json "$LearningServiceUrl/api/enrollments?studentId=$($registered.id)&courseId=$($programmingCourse.id)" "POST" @{}
$programmingTasks = Get-Json "$AssessmentServiceUrl/internal/tasks?courseId=$($programmingCourse.id)" $headers
$programmingTask = @($programmingTasks | Where-Object { $_.type -eq "programming" })[0]
Assert-True ($null -ne $programmingTask) "Programming fixture task is missing"
$judge = Send-SessionJson "$BaseUrl/api/v2/judge/submit" @{ taskId=$programmingTask.id; language="python"; code="a,b=map(int,input().split())`nprint(a+b)" }
Assert-True ($judge.status -eq "AC") "Programming judge did not return AC: $($judge.status)"
$judgeSubmissions = Get-Json "$AssessmentServiceUrl/internal/submissions/student/$($registered.id)" $headers
$savedJudge = @($judgeSubmissions | Where-Object { $_.taskId -eq $programmingTask.id -and $_.status -eq "graded" })[0]
Assert-True ($null -ne $savedJudge -and $savedJudge.judgeResult -eq "AC") "BFF did not bind the judged submission to the logged-in student"

Write-Host "[8/8] Gateway page flow"
$studentPage = Invoke-WebRequestCompat @{
    Uri = "$BaseUrl/student/course/my"
    WebSession = $pageSession
    UseBasicParsing = $true
}
Assert-True ($studentPage.StatusCode -eq 200 -and [string]$studentPage.Content -notmatch "Whitelabel Error Page") "Authenticated BFF page failed"
foreach ($path in @("/", "/student/course/selection", "/student/course/my", "/teacher/course/manage", "/admin/dashboard")) {
    $status = Get-StatusWithoutRedirect "$BaseUrl$path"
    Assert-True ($status -in @(200,302,303)) "Page route failed: $path (HTTP $status)"
}
Write-Host "Microservice business regression passed ($checks checks)."
