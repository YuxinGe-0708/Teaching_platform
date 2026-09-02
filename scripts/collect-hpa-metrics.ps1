[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory,
    [Parameter(Mandatory = $true)]
    [string]$StopFile,
    [int]$IntervalSeconds = 5,
    [string]$Namespace = 'teaching-platform'
)

$ErrorActionPreference = 'Continue'
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$timelinePath = Join-Path $OutputDirectory 'hpa-timeline.csv'
$errorsPath = Join-Path $OutputDirectory 'collector-errors.log'

'timestamp_utc,hpa_current_replicas,hpa_desired_replicas,hpa_cpu_current_pct,hpa_cpu_target_pct,deployment_replicas,deployment_ready_replicas,pod_name,pod_phase,pod_ready,pod_restarts,cpu_millicores,memory_mib' |
    Set-Content -Encoding utf8 $timelinePath

function Convert-CpuToMillicores([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return '' }
    if ($Value.EndsWith('m')) { return [double]$Value.TrimEnd('m') }
    return [math]::Round(([double]$Value) * 1000, 3)
}

function Convert-MemoryToMiB([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return '' }
    if ($Value.EndsWith('Ki')) { return [math]::Round(([double]$Value.TrimEnd('K','i')) / 1024, 3) }
    if ($Value.EndsWith('Mi')) { return [double]$Value.TrimEnd('M','i') }
    if ($Value.EndsWith('Gi')) { return [math]::Round(([double]$Value.TrimEnd('G','i')) * 1024, 3) }
    return ''
}

while (-not (Test-Path -LiteralPath $StopFile)) {
    $timestamp = (Get-Date).ToUniversalTime().ToString('o')
    try {
        $hpa = kubectl -n $Namespace get hpa user-service -o json 2>> $errorsPath | ConvertFrom-Json
        $deployment = kubectl -n $Namespace get deployment user-service -o json 2>> $errorsPath | ConvertFrom-Json
        $podList = kubectl -n $Namespace get pods -l app=user-service -o json 2>> $errorsPath | ConvertFrom-Json
        $topByPod = @{}
        $topLines = @(kubectl -n $Namespace top pods -l app=user-service --no-headers 2>> $errorsPath)
        foreach ($line in $topLines) {
            $parts = $line -split '\s+'
            if ($parts.Count -ge 3) {
                $topByPod[$parts[0]] = @{ Cpu = $parts[1]; Memory = $parts[2] }
            }
        }

        $currentCpu = ''
        if ($hpa.status.currentMetrics.Count -gt 0) {
            $currentCpu = $hpa.status.currentMetrics[0].resource.current.averageUtilization
        }
        $targetCpu = $hpa.spec.metrics[0].resource.target.averageUtilization

        foreach ($pod in $podList.items) {
            $containerStatuses = @($pod.status.containerStatuses)
            $ready = ($containerStatuses.Count -gt 0) -and (($containerStatuses | Where-Object { -not $_.ready }).Count -eq 0)
            $restarts = ($containerStatuses | Measure-Object -Property restartCount -Sum).Sum
            $usage = $topByPod[$pod.metadata.name]
            $row = [pscustomobject]@{
                timestamp_utc            = $timestamp
                hpa_current_replicas      = $hpa.status.currentReplicas
                hpa_desired_replicas      = $hpa.status.desiredReplicas
                hpa_cpu_current_pct       = $currentCpu
                hpa_cpu_target_pct        = $targetCpu
                deployment_replicas       = $deployment.status.replicas
                deployment_ready_replicas = $deployment.status.readyReplicas
                pod_name                  = $pod.metadata.name
                pod_phase                 = $pod.status.phase
                pod_ready                 = $ready
                pod_restarts              = $restarts
                cpu_millicores            = Convert-CpuToMillicores $usage.Cpu
                memory_mib                = Convert-MemoryToMiB $usage.Memory
            }
            $row | ConvertTo-Csv -NoTypeInformation | Select-Object -Skip 1 | Add-Content -Encoding utf8 $timelinePath
        }
    }
    catch {
        "${timestamp}`t$($_.Exception.Message)" | Add-Content -Encoding utf8 $errorsPath
    }
    Start-Sleep -Seconds $IntervalSeconds
}

