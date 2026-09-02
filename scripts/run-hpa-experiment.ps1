[CmdletBinding()]
param(
    [string]$K6Path = 'k6',
    [string]$KindPath = 'kind',
    [string]$ClusterName = 'teaching-platform-hpa',
    [string]$ImageTag = 'hpa-experiment',
    [string]$MySqlImage = 'mysql:8.0.40',
    [int]$Runs = 3,
    [int]$PeakVus = 120,
    [int]$ScaleDownTimeoutSeconds = 300,
    [switch]$ProvisionOnly,
    [switch]$SkipProvision
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$namespace = 'teaching-platform'
$resultsRoot = Join-Path $repoRoot 'results\hpa'
$imageName = "teaching-platform-user-service:$ImageTag"
New-Item -ItemType Directory -Force -Path $resultsRoot | Out-Null

function Invoke-Checked([scriptblock]$Command, [string]$Description) {
    & $Command
    if ($LASTEXITCODE -ne 0) { throw "$Description failed with exit code $LASTEXITCODE" }
}

function Wait-ForReplicaCount([int]$Count, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $current = kubectl -n $namespace get hpa user-service -o jsonpath='{.status.currentReplicas}' 2>$null
        $ready = kubectl -n $namespace get deployment user-service -o jsonpath='{.status.readyReplicas}' 2>$null
        if ([int]$current -eq $Count -and [int]$ready -eq $Count) { return $true }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)
    return $false
}

Push-Location $repoRoot
try {
    if (-not $SkipProvision) {
        $clusters = @(& $KindPath get clusters)
        if ($clusters -notcontains $ClusterName) {
            Invoke-Checked { & $KindPath create cluster --name $ClusterName --config k8s/hpa/kind-config.yaml } 'kind cluster creation'
        }

        Invoke-Checked { mvn -f services/user-service/pom.xml -DskipTests package } 'user-service Maven package'
        Invoke-Checked { docker build -f services/user-service/Dockerfile.runtime -t $imageName services/user-service } 'user-service image build'
        Invoke-Checked { & $KindPath load docker-image $imageName --name $ClusterName } 'loading user-service image into kind'

        kubectl apply -f k8s/namespace.yaml | Out-Host
        kubectl -n $namespace create secret generic teaching-platform-secrets `
            --from-literal=MYSQL_ROOT_PASSWORD=hpa-experiment-password `
            --from-literal=INTERNAL_API_KEY=hpa-internal-key `
            --dry-run=client -o yaml | kubectl apply -f - | Out-Host
        kubectl -n $namespace create configmap user-service-schema `
            --from-file=schema-user.sql=services/user-service/src/main/resources/db/schema-user.sql `
            --dry-run=client -o yaml | kubectl apply -f - | Out-Host
        kubectl -n $namespace create configmap user-service-seed `
            --from-file=seed-user.sql=services/user-service/src/main/resources/db/seed-user.sql `
            --dry-run=client -o yaml | kubectl apply -f - | Out-Host

        # mysql.yaml also references the other services' schema/seed ConfigMaps.
        kubectl -n $namespace create configmap learning-service-schema `
            --from-file=schema-learning.sql=services/learning-service/src/main/resources/db/schema-learning.sql `
            --dry-run=client -o yaml | kubectl apply -f - | Out-Host
        kubectl -n $namespace create configmap assessment-service-schema `
            --from-file=schema-assessment.sql=services/assessment-service/src/main/resources/db/schema-assessment.sql `
            --dry-run=client -o yaml | kubectl apply -f - | Out-Host
        kubectl -n $namespace create configmap learning-service-seed `
            --from-file=seed-learning.sql=services/learning-service/src/main/resources/db/seed-learning.sql `
            --dry-run=client -o yaml | kubectl apply -f - | Out-Host
        kubectl -n $namespace create configmap assessment-service-seed `
            --from-file=seed-assessment.sql=services/assessment-service/src/main/resources/db/seed-assessment.sql `
            --dry-run=client -o yaml | kubectl apply -f - | Out-Host
        kubectl apply -f k8s/mysql.yaml | Out-Host
        kubectl -n $namespace set image deployment/mysql "mysql=$MySqlImage" | Out-Host
        Invoke-Checked { kubectl -n $namespace rollout status deployment/mysql --timeout=8m } 'MySQL rollout'

        $deployment = Get-Content -Raw services/user-service/k8s/user-service/deployment.yaml
        $deployment = $deployment.Replace('__SWR_REGISTRY__/__SWR_ORG__/teaching-platform-user-service:__IMAGE_TAG__', $imageName)
        $deployment | kubectl apply -f - | Out-Host
        Invoke-Checked { kubectl -n $namespace rollout status deployment/user-service --timeout=8m } 'user-service rollout'
    }

    kubectl apply -f k8s/hpa/user-service-hpa.yaml | Out-Host
    Invoke-Checked { kubectl wait --for=condition=Available deployment/user-service -n $namespace --timeout=5m } 'user-service availability'
    Invoke-Checked { kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes *> $null } 'Metrics API availability (run scripts/install-kind-metrics-server.ps1 first)'
    if ($ProvisionOnly) { return }

    $portOut = Join-Path $resultsRoot 'port-forward.stdout.log'
    $portErr = Join-Path $resultsRoot 'port-forward.stderr.log'
    $portForward = Start-Process kubectl -ArgumentList @('-n', $namespace, 'port-forward', 'service/user-service', '18082:8082') -PassThru -WindowStyle Hidden -RedirectStandardOutput $portOut -RedirectStandardError $portErr
    try {
        $ready = $false
        foreach ($attempt in 1..30) {
            try {
                $response = Invoke-WebRequest -UseBasicParsing http://127.0.0.1:18082/actuator/health -TimeoutSec 3
                if ($response.StatusCode -eq 200) { $ready = $true; break }
            } catch { Start-Sleep -Seconds 2 }
        }
        if (-not $ready) { throw 'user-service port-forward did not become ready' }

        foreach ($runNumber in 1..$Runs) {
            $runName = 'run-{0:d2}' -f $runNumber
            $runDirectory = Join-Path $resultsRoot $runName
            New-Item -ItemType Directory -Force -Path $runDirectory | Out-Null

            kubectl -n $namespace scale deployment user-service --replicas=1 | Out-Host
            if (-not (Wait-ForReplicaCount -Count 1 -TimeoutSeconds $ScaleDownTimeoutSeconds)) {
                throw "$runName did not return to one ready replica before the run"
            }
            Start-Sleep -Seconds 30

            kubectl -n $namespace get hpa user-service -o yaml | Set-Content -Encoding utf8 (Join-Path $runDirectory 'hpa-before.yaml')
            kubectl -n $namespace get pods -l app=user-service -o wide | Set-Content -Encoding utf8 (Join-Path $runDirectory 'pods-before.txt')
            $stopFile = Join-Path $runDirectory 'stop-collector.signal'
            Remove-Item -Force -ErrorAction SilentlyContinue $stopFile
            $collectorOut = Join-Path $runDirectory 'collector.stdout.log'
            $collectorErr = Join-Path $runDirectory 'collector.stderr.log'
            $collector = Start-Process powershell -ArgumentList @(
                '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'collect-hpa-metrics.ps1'),
                '-OutputDirectory', $runDirectory, '-StopFile', $stopFile, '-IntervalSeconds', '5', '-Namespace', $namespace
            ) -PassThru -WindowStyle Hidden -RedirectStandardOutput $collectorOut -RedirectStandardError $collectorErr

            try {
                $env:BASE_URL = 'http://127.0.0.1:18082'
                $env:BASE_VUS = '2'
                $env:PEAK_VUS = [string]$PeakVus
                $env:BASELINE_DURATION = '30s'
                $env:RAMP_DURATION = '30s'
                $env:PEAK_DURATION = '150s'
                $env:RAMP_DOWN_DURATION = '15s'
                $rawPath = Join-Path $runDirectory 'k6-raw.json'
                $summaryPath = Join-Path $runDirectory 'k6-summary.json'
                $k6Log = Join-Path $runDirectory 'k6-console.log'
                & $K6Path run --out "json=$rawPath" --summary-export $summaryPath scripts/load-test-hpa.js *>&1 | Tee-Object -FilePath $k6Log
                $k6Exit = $LASTEXITCODE

                $deadline = (Get-Date).AddSeconds($ScaleDownTimeoutSeconds)
                do {
                    $replicas = kubectl -n $namespace get hpa user-service -o jsonpath='{.status.currentReplicas}'
                    if ([int]$replicas -eq 1) { break }
                    Start-Sleep -Seconds 5
                } while ((Get-Date) -lt $deadline)
                Start-Sleep -Seconds 10
                if ($k6Exit -ne 0) { throw "$runName k6 exited with $k6Exit" }
            }
            finally {
                New-Item -ItemType File -Force -Path $stopFile | Out-Null
                if (-not $collector.HasExited) { $collector.WaitForExit(15000) }
                if (-not $collector.HasExited) { Stop-Process -Id $collector.Id -Force }
                kubectl -n $namespace get hpa user-service -o yaml | Set-Content -Encoding utf8 (Join-Path $runDirectory 'hpa-after.yaml')
                kubectl -n $namespace describe hpa user-service | Set-Content -Encoding utf8 (Join-Path $runDirectory 'hpa-describe.txt')
                kubectl -n $namespace get pods -l app=user-service -o wide | Set-Content -Encoding utf8 (Join-Path $runDirectory 'pods-after.txt')
                kubectl -n $namespace get events --sort-by=.lastTimestamp | Set-Content -Encoding utf8 (Join-Path $runDirectory 'events.txt')
                Remove-Item Env:BASE_URL,Env:BASE_VUS,Env:PEAK_VUS,Env:BASELINE_DURATION,Env:RAMP_DURATION,Env:PEAK_DURATION,Env:RAMP_DOWN_DURATION -ErrorAction SilentlyContinue
            }
        }
    }
    finally {
        if ($portForward -and -not $portForward.HasExited) { Stop-Process -Id $portForward.Id -Force }
    }

    & (Join-Path $PSScriptRoot 'summarize-hpa-results.ps1') -ResultsDirectory $resultsRoot
}
finally {
    Pop-Location
}
