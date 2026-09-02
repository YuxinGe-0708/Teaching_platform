[CmdletBinding()]
param(
    [ValidateSet("monolith", "microservices")]
    [string]$Target = "microservices",
    [string]$BaseUrl = "",
    [string]$Username = "",
    [string]$Password = "",
    [string]$CourseId = "auto",
    [string]$JudgeTaskId = "",
    [int]$Runs = 3,
    [int]$Concurrency = 10,
    [int]$DurationSeconds = 60,
    [int]$WarmupSeconds = 15,
    [string]$PythonPath = "python",
    [string]$ResultsRoot = "results\performance",
    [string]$ContainerPattern = "",
    [string]$ExperimentId = "",
    [string]$SourceCommit = "",
    [string]$SourceBranch = "",
    [switch]$NoJudgeTaskId,
    [switch]$SkipResourceCollection
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$configPath = Join-Path $repoRoot "tests\performance\perf-test-data.json"
$config = Get-Content -Raw $configPath | ConvertFrom-Json

if (-not $BaseUrl) {
    $targetConfig = $config.$Target
    $BaseUrl = $targetConfig.baseUrl
}
if (-not $Username) {
    $Username = $config.$Target.username
}
if (-not $Password) {
    $Password = $config.$Target.password
}
if ($CourseId -eq "auto" -and $config.$Target.courseId) {
    $CourseId = [string]$config.$Target.courseId
}
if (-not $JudgeTaskId -and $config.$Target.judgeTaskId) {
    $JudgeTaskId = [string]$config.$Target.judgeTaskId
}
if ($NoJudgeTaskId) {
    $JudgeTaskId = ""
}
if (-not $ContainerPattern) {
    if ($Target -eq "monolith") {
        $ContainerPattern = "teachplatform-mysql|teaching-platform-backend|teaching-platform-frontend"
    } else {
        $ContainerPattern = "microservices-mysql|user-service|learning-service|assessment-service|web-bff|gateway"
    }
}
if ($Runs -lt 3) {
    throw "Runs must be at least 3."
}

$timestamp = if ($ExperimentId) {
    $ExperimentId
} else {
    (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
}
$targetRoot = Join-Path (Join-Path $repoRoot $ResultsRoot) "$timestamp\$Target"
New-Item -ItemType Directory -Force -Path $targetRoot | Out-Null

function Invoke-GitValue([string[]]$Arguments) {
    $value = & git @Arguments
    if ($LASTEXITCODE -ne 0) { return "unknown" }
    return ($value | Out-String).Trim()
}

$metadataBase = [ordered]@{
    experiment_id = $timestamp
    target = $Target
    base_url = $BaseUrl
    username = $Username
    password = "<redacted>"
    course_id = $CourseId
    judge_task_id = $JudgeTaskId
    runs = $Runs
    concurrency = $Concurrency
    warmup_seconds = $WarmupSeconds
    duration_seconds = $DurationSeconds
    dataset = $config.dataset
    git_commit = if ($SourceCommit) { $SourceCommit } else { Invoke-GitValue @("rev-parse", "HEAD") }
    git_branch = if ($SourceBranch) { $SourceBranch } else { Invoke-GitValue @("branch", "--show-current") }
    host = [ordered]@{
        os = [System.Environment]::OSVersion.VersionString
        processor_count = [Environment]::ProcessorCount
    }
}
$metadataBase | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $targetRoot "experiment-metadata.json")

$benchmarkPath = Join-Path $repoRoot "tests\performance\run_http_benchmark.py"
$collectorPath = Join-Path $repoRoot "tests\performance\collect_resources.py"
$scenarios = @("login", "course_detail", "judge_submit")

Push-Location $repoRoot
try {
    foreach ($scenario in $scenarios) {
        foreach ($runNumber in 1..$Runs) {
            $runDirectory = Join-Path $targetRoot "$scenario\run-$('{0:d2}' -f $runNumber)"
            New-Item -ItemType Directory -Force -Path $runDirectory | Out-Null
            $runMetadata = [ordered]@{}
            foreach ($key in $metadataBase.Keys) {
                $runMetadata[$key] = $metadataBase[$key]
            }
            $runMetadata.scenario = $scenario
            $runMetadata.run_number = $runNumber
            $runMetadata | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $runDirectory "experiment-metadata.json")

            $stopFile = Join-Path $runDirectory "stop-resource-collector.signal"
            Remove-Item -Force -ErrorAction SilentlyContinue $stopFile
            $collector = $null
            if (-not $SkipResourceCollection) {
                $collectorOutput = Join-Path $runDirectory "resource-collector.stdout.log"
                $collectorError = Join-Path $runDirectory "resource-collector.stderr.log"
                $collectorArguments = @(
                    $collectorPath,
                    "--output", (Join-Path $runDirectory "resources.csv"),
                    "--duration-seconds", ($WarmupSeconds + $DurationSeconds + 30),
                    "--interval-seconds", "1",
                    "--stop-file", $stopFile,
                    "--container-pattern", $ContainerPattern
                )
                $collector = Start-Process -FilePath $PythonPath -ArgumentList $collectorArguments `
                    -PassThru -WindowStyle Hidden -RedirectStandardOutput $collectorOutput `
                    -RedirectStandardError $collectorError
            }

            try {
                $benchmarkOutput = Join-Path $runDirectory "benchmark.stdout.log"
                $benchmarkArguments = @(
                    $benchmarkPath,
                    "--base-url", $BaseUrl,
                    "--target", $Target,
                    "--scenario", $scenario,
                    "--run-number", $runNumber,
                    "--concurrency", $Concurrency,
                    "--duration-seconds", $DurationSeconds,
                    "--warmup-seconds", $WarmupSeconds,
                    "--username", $Username,
                    "--password", $Password,
                    "--course-id", $CourseId,
                    "--output", (Join-Path $runDirectory "benchmark.json")
                )
                if ($JudgeTaskId) {
                    $benchmarkArguments += @("--judge-task-id", $JudgeTaskId)
                }
                $benchmarkArguments += @("--resource-file", (Join-Path $runDirectory "resources.csv"))
                & $PythonPath @benchmarkArguments *> $benchmarkOutput
                $benchmarkExit = $LASTEXITCODE
                Get-Content $benchmarkOutput | Write-Host
                if ($benchmarkExit -ne 0) {
                    throw "$Target/$scenario/run-$runNumber benchmark failed with exit code $benchmarkExit. See $benchmarkOutput"
                }
            }
            finally {
                if (-not $SkipResourceCollection) {
                    New-Item -ItemType File -Force -Path $stopFile | Out-Null
                    if ($collector -and -not $collector.HasExited) {
                        $collector.WaitForExit(30000)
                    }
                    if ($collector -and -not $collector.HasExited) {
                        Stop-Process -Id $collector.Id -Force
                    }
                }
            }
        }
    }
}
finally {
    Pop-Location
}

& $PythonPath (Join-Path $repoRoot "tests\performance\summarize_performance.py") `
    "--results-root" (Join-Path (Join-Path $repoRoot $ResultsRoot) $timestamp)
