param(
    [string]$UserUrl = $env:E2E_USER_SERVICE_URL,
    [string]$LearningUrl = $env:E2E_LEARNING_SERVICE_URL,
    [string]$AssessmentUrl = $env:E2E_ASSESSMENT_SERVICE_URL,
    [string]$InternalApiKey = $env:INTERNAL_API_KEY,
    [string]$ReportFile = "ci-artifacts\e2e-report.json"
)

$ErrorActionPreference = "Stop"

if (-not $UserUrl) { $UserUrl = "http://localhost:8082" }
if (-not $LearningUrl) { $LearningUrl = "http://localhost:8083" }
if (-not $AssessmentUrl) { $AssessmentUrl = "http://localhost:8084" }
if (-not $InternalApiKey) { $InternalApiKey = "dev-internal-key" }

New-Item -ItemType Directory -Force (Split-Path $ReportFile) | Out-Null

python -m pip install -r tests/e2e/requirements.txt
python -m pytest tests/e2e `
  --user-url $UserUrl `
  --learning-url $LearningUrl `
  --assessment-url $AssessmentUrl `
  --internal-api-key $InternalApiKey `
  --report-file $ReportFile
