param(
    [ValidateSet("monolith", "microservices")]
    [string]$Mode = "monolith",
    [int]$Runs = 3,
    [int]$Concurrency = 10,
    [int]$DurationSeconds = 60
)

$ErrorActionPreference = "Stop"
$scriptPath = Join-Path $PSScriptRoot "run-performance-suite.ps1"
& $scriptPath -Target $Mode -Runs $Runs -Concurrency $Concurrency -DurationSeconds $DurationSeconds
