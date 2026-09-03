[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ResultsDirectory
)

$ErrorActionPreference = 'Stop'
$summaryRows = foreach ($runDirectory in Get-ChildItem -LiteralPath $ResultsDirectory -Directory | Where-Object Name -Match '^run-\d+$' | Sort-Object Name) {
    $summaryPath = Join-Path $runDirectory.FullName 'k6-summary.json'
    $timelinePath = Join-Path $runDirectory.FullName 'hpa-timeline.csv'
    if (-not (Test-Path $summaryPath) -or -not (Test-Path $timelinePath)) { continue }

    $k6 = Get-Content -Raw $summaryPath | ConvertFrom-Json
    $timeline = @(Import-Csv $timelinePath)
    $metadataPath = Join-Path $runDirectory.FullName 'experiment-metadata.json'
    $metadata = if (Test-Path $metadataPath) {
        Get-Content -Raw $metadataPath | ConvertFrom-Json
    } else {
        $rawPath = Join-Path $runDirectory.FullName 'k6-raw.json'
        $firstPoints = @(Get-Content $rawPath -TotalCount 100 | ForEach-Object {
            try { $_ | ConvertFrom-Json } catch { $null }
        } | Where-Object { $_.type -eq 'Point' -and $_.data.time })
        $lastPoints = @(Get-Content $rawPath -Tail 100 | ForEach-Object {
            try { $_ | ConvertFrom-Json } catch { $null }
        } | Where-Object { $_.type -eq 'Point' -and $_.data.time })
        $value = [pscustomobject]@{
            load_start_utc = ([datetime]($firstPoints | Select-Object -First 1).data.time).ToUniversalTime().ToString('o')
            load_end_utc = ([datetime]($lastPoints | Select-Object -Last 1).data.time).ToUniversalTime().ToString('o')
            peak_vus = $k6.metrics.vus.max
        }
        $value | ConvertTo-Json | Set-Content -Encoding utf8 $metadataPath
        $value
    }
    $podSamples = @($timeline | Where-Object pod_name)
    $timeSamples = @($podSamples | Group-Object timestamp_utc | ForEach-Object {
        [pscustomobject]@{
            Timestamp = [datetime]$_.Name
            Replicas = ($_.Group | Measure-Object -Property hpa_current_replicas -Maximum).Maximum
            Cpu = ($_.Group | Where-Object hpa_cpu_current_pct | Measure-Object -Property hpa_cpu_current_pct -Maximum).Maximum
        }
    } | Sort-Object Timestamp)

    $maxReplicas = ($timeSamples | Measure-Object -Property Replicas -Maximum).Maximum
    $maxCpu = ($timeSamples | Measure-Object -Property Cpu -Maximum).Maximum
    $firstScaleUp = $timeSamples | Where-Object { [int]$_.Replicas -gt 1 } | Select-Object -First 1
    $loadStart = [datetime]$metadata.load_start_utc
    $loadEnd = [datetime]$metadata.load_end_utc
    $scaledDown = $false
    $scaleDownSeconds = ''
    $down = $timeSamples | Where-Object { $_.Timestamp -gt $loadEnd -and [int]$_.Replicas -lt [int]$maxReplicas } | Select-Object -First 1
    if ($down) {
        $scaledDown = $true
        $scaleDownSeconds = [math]::Round(($down.Timestamp - $loadEnd).TotalSeconds, 1)
    }

    $metrics = $k6.metrics
    # k6 v2 writes metric values directly; older k6 releases nested them in
    # a "values" object. Support both so archived experiments stay readable.
    $requestMetric = if ($metrics.http_reqs.values) { $metrics.http_reqs.values } else { $metrics.http_reqs }
    $durationMetric = if ($metrics.http_req_duration.values) { $metrics.http_req_duration.values } else { $metrics.http_req_duration }
    $failedMetric = if ($metrics.http_req_failed.values) { $metrics.http_req_failed.values } else { $metrics.http_req_failed }
    $requests = [double]$requestMetric.count
    $rate = [double]$requestMetric.rate
    $failedRate = if ($null -ne $failedMetric.rate) { [double]$failedMetric.rate } else { [double]$failedMetric.value }
    [pscustomobject]@{
        run                    = $runDirectory.Name
        requests               = [math]::Round($requests, 0)
        throughput_rps         = [math]::Round($rate, 3)
        avg_response_ms        = [math]::Round([double]$durationMetric.avg, 3)
        p95_response_ms        = [math]::Round([double]$durationMetric.'p(95)', 3)
        errors                 = [math]::Round($requests * $failedRate, 0)
        error_rate_pct         = [math]::Round($failedRate * 100, 4)
        max_cpu_pct            = $maxCpu
        initial_pods           = if ($timeSamples.Count) { $timeSamples[0].Replicas } else { '' }
        max_pods               = $maxReplicas
        scaled_up              = [bool]$firstScaleUp
        scale_up_seconds       = if ($firstScaleUp) { [math]::Round(($firstScaleUp.Timestamp - $loadStart).TotalSeconds, 1) } else { '' }
        scaled_down_after_peak = $scaledDown
        scale_down_seconds     = $scaleDownSeconds
    }
}

$summaryPath = Join-Path $ResultsDirectory 'summary.csv'
$summaryRows | Export-Csv -NoTypeInformation -Encoding utf8 $summaryPath

$completedRows = @($summaryRows)
if ($completedRows.Count -eq 0) {
    Write-Warning "No completed experiment results were found in $ResultsDirectory"
    return
}

Write-Host ''
Write-Host '=== Key performance metrics ==='
$completedRows |
    Select-Object run,throughput_rps,avg_response_ms,p95_response_ms,error_rate_pct |
    Format-Table -AutoSize | Out-Host

$averages = [pscustomobject]@{
    completed_runs          = $completedRows.Count
    avg_throughput_rps      = [math]::Round(($completedRows | Measure-Object throughput_rps -Average).Average, 3)
    avg_response_ms         = [math]::Round(($completedRows | Measure-Object avg_response_ms -Average).Average, 3)
    avg_p95_response_ms     = [math]::Round(($completedRows | Measure-Object p95_response_ms -Average).Average, 3)
    avg_error_rate_pct      = [math]::Round(($completedRows | Measure-Object error_rate_pct -Average).Average, 4)
}

Write-Host '=== Average across completed runs ==='
$averages | Format-List | Out-Host

Write-Host "Full summary saved to $summaryPath"
