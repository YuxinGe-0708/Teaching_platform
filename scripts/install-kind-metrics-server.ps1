[CmdletBinding()]
param(
    [string]$Version = 'v0.9.0'
)

$ErrorActionPreference = 'Stop'
$manifestPath = Join-Path $env:TEMP "metrics-server-$Version.yaml"
$downloadUrl = "https://github.com/kubernetes-sigs/metrics-server/releases/download/$Version/components.yaml"

Invoke-WebRequest -UseBasicParsing $downloadUrl -OutFile $manifestPath
kubectl apply -f $manifestPath | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Metrics Server manifest apply failed' }

$deployment = kubectl -n kube-system get deployment metrics-server -o json | ConvertFrom-Json
$arguments = @($deployment.spec.template.spec.containers[0].args)
if ($arguments -notcontains '--kubelet-insecure-tls') {
    $patchPath = Join-Path $env:TEMP "metrics-server-kind-tls-$PID.json"
    $patch = @'
[
  {
    "op": "add",
    "path": "/spec/template/spec/containers/0/args/-",
    "value": "--kubelet-insecure-tls"
  }
]
'@

    try {
        # --patch-file avoids native-command JSON quoting differences between
        # Windows PowerShell 5.1 and PowerShell 7.
        Set-Content -LiteralPath $patchPath -Value $patch -Encoding Ascii
        kubectl -n kube-system patch deployment metrics-server --type=json `
            --patch-file $patchPath | Out-Host
        $patchExitCode = $LASTEXITCODE
    }
    finally {
        Remove-Item -LiteralPath $patchPath -Force -ErrorAction SilentlyContinue
    }

    if ($patchExitCode -ne 0) { throw 'Metrics Server kind TLS patch failed' }
}

kubectl -n kube-system rollout status deployment/metrics-server --timeout=5m | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Metrics Server rollout failed' }

$deadline = (Get-Date).AddMinutes(2)
do {
    kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes *> $null
    if ($LASTEXITCODE -eq 0) {
        kubectl top nodes
        exit 0
    }
    Start-Sleep -Seconds 5
} while ((Get-Date) -lt $deadline)

throw 'Metrics API did not become available within two minutes'
