[CmdletBinding()]
param(
    [string]$KindPath = 'kind',
    [string]$ClusterName = 'teaching-platform-hpa',
    [string]$ImageTag = 'hpa-experiment',
    [string]$MySqlImage = 'mysql:8.0.40',
    [int]$Runs = 3,
    [int]$PeakVus = 120,
    [int]$ScaleDownTimeoutSeconds = 300,
    [switch]$ProvisionOnly,
    [switch]$SkipProvision,
    [switch]$KeepRawJson
)

$ErrorActionPreference = 'Stop'

if ($env:OS -eq 'Windows_NT') {
    chcp.com 65001 > $null
}
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom

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

    kubectl -n $namespace create configmap hpa-load-test-script `
        --from-file=load-test-hpa.js=scripts/load-test-hpa.js `
        --dry-run=client -o yaml | kubectl apply -f - | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'k6 test script ConfigMap apply failed' }

    # Recreate the disposable Pod so every run mounts the current script and
    # starts with an empty result volume.
    kubectl -n $namespace delete pod hpa-load-generator --ignore-not-found --wait=true | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'old k6 load generator Pod cleanup failed' }
    kubectl apply -f k8s/hpa/k6-load-generator.yaml | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'k6 load generator Pod apply failed' }
    Invoke-Checked {
        kubectl -n $namespace wait --for=condition=Ready pod/hpa-load-generator --timeout=5m
    } 'k6 load generator readiness'

    if ($ProvisionOnly) { return }

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
                $rawPath = Join-Path $runDirectory 'k6-raw.json'
                $summaryPath = Join-Path $runDirectory 'k6-summary.json'
                $k6Log = Join-Path $runDirectory 'k6-console.log'
                # kubectl cp treats the colon in a Windows absolute path as a
                # remote-path separator, so use paths relative to $repoRoot.
                $rawCopyPath = Join-Path (Join-Path 'results\hpa' $runName) 'k6-raw.json'
                $summaryCopyPath = Join-Path (Join-Path 'results\hpa' $runName) 'k6-summary.json'
                kubectl -n $namespace exec hpa-load-generator -- sh -c 'rm -f /results/k6-raw.json /results/k6-summary.json'
                if ($LASTEXITCODE -ne 0) { throw "$runName could not clear the k6 result volume" }

                $previousErrorActionPreference = $ErrorActionPreference
                try {
                    # k6 writes request-timeout warnings to stderr. Windows
                    # PowerShell 5.1 turns native stderr into ErrorRecords, so
                    # temporarily keep them non-terminating and let k6 finish.
                    $ErrorActionPreference = 'Continue'
                    kubectl -n $namespace exec hpa-load-generator -- env `
                        BASE_URL=http://user-service:8082 `
                        BASE_VUS=2 `
                        PEAK_VUS=$PeakVus `
                        BASELINE_DURATION=30s `
                        RAMP_DURATION=30s `
                        PEAK_DURATION=150s `
                        RAMP_DOWN_DURATION=15s `
                        k6 run `
                        --out json=/results/k6-raw.json `
                        --summary-export=/results/k6-summary.json `
                        /scripts/load-test-hpa.js *>&1 | Tee-Object -FilePath $k6Log
                    $k6Exit = $LASTEXITCODE
                }
                finally {
                    $ErrorActionPreference = $previousErrorActionPreference
                }

                kubectl -n $namespace cp 'hpa-load-generator:/results/k6-raw.json' $rawCopyPath
                if ($LASTEXITCODE -ne 0) { throw "$runName raw k6 result copy failed" }
                kubectl -n $namespace cp 'hpa-load-generator:/results/k6-summary.json' $summaryCopyPath
                if ($LASTEXITCODE -ne 0) { throw "$runName k6 summary copy failed" }
                try {
                    Get-Content -LiteralPath $summaryPath -Raw | ConvertFrom-Json | Out-Null
                }
                catch {
                    throw "$runName k6 summary is not valid JSON"
                }

                $deadline = (Get-Date).AddSeconds($ScaleDownTimeoutSeconds)
                do {
                    $replicas = kubectl -n $namespace get hpa user-service -o jsonpath='{.status.currentReplicas}'
                    if ([int]$replicas -eq 1) { break }
                    Start-Sleep -Seconds 5
                } while ((Get-Date) -lt $deadline)
                Start-Sleep -Seconds 10
                if ($k6Exit -ne 0) {
                    Write-Warning "$runName k6 exited with $k6Exit; complete results were retained for error-rate analysis"
                }
            }
            finally {
                New-Item -ItemType File -Force -Path $stopFile | Out-Null
                if (-not $collector.HasExited) { $collector.WaitForExit(15000) }
                if (-not $collector.HasExited) { Stop-Process -Id $collector.Id -Force }
                kubectl -n $namespace get hpa user-service -o yaml | Set-Content -Encoding utf8 (Join-Path $runDirectory 'hpa-after.yaml')
                kubectl -n $namespace describe hpa user-service | Set-Content -Encoding utf8 (Join-Path $runDirectory 'hpa-describe.txt')
                kubectl -n $namespace get pods -l app=user-service -o wide | Set-Content -Encoding utf8 (Join-Path $runDirectory 'pods-after.txt')
                kubectl -n $namespace get events --sort-by=.lastTimestamp | Set-Content -Encoding utf8 (Join-Path $runDirectory 'events.txt')
            }
    }

    & (Join-Path $PSScriptRoot 'summarize-hpa-results.ps1') -ResultsDirectory $resultsRoot

    if (-not $KeepRawJson) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        foreach ($runDirectory in Get-ChildItem -LiteralPath $resultsRoot -Directory | Where-Object Name -Match '^run-\d+$') {
            $rawPath = Join-Path $runDirectory.FullName 'k6-raw.json'
            if (-not (Test-Path -LiteralPath $rawPath)) { continue }

            $rawLength = (Get-Item -LiteralPath $rawPath).Length
            $archivePath = "$rawPath.zip"
            $temporaryArchivePath = "$rawPath.tmp.zip"
            Remove-Item -LiteralPath $temporaryArchivePath -Force -ErrorAction SilentlyContinue
            Compress-Archive -LiteralPath $rawPath -DestinationPath $temporaryArchivePath -CompressionLevel Optimal

            if (-not (Test-Path -LiteralPath $temporaryArchivePath) -or
                (Get-Item -LiteralPath $temporaryArchivePath).Length -eq 0) {
                throw "Raw k6 result compression failed for $($runDirectory.Name)"
            }

            $archive = [System.IO.Compression.ZipFile]::OpenRead($temporaryArchivePath)
            try {
                $rawEntry = $archive.Entries | Where-Object Name -EQ 'k6-raw.json' | Select-Object -First 1
                if ($null -eq $rawEntry -or $rawEntry.Length -ne $rawLength) {
                    throw "Raw k6 result archive verification failed for $($runDirectory.Name)"
                }
            }
            finally {
                $archive.Dispose()
            }

            Move-Item -LiteralPath $temporaryArchivePath -Destination $archivePath -Force
            Remove-Item -LiteralPath $rawPath -Force
            Write-Host "Compressed $($runDirectory.Name) raw result to k6-raw.json.zip"
        }
    }
}
finally {
    Pop-Location
}
