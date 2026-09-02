param(
    [string]$ComposeFile = "docker-compose.microservices.yml"
)

$ErrorActionPreference = "Stop"

foreach ($service in @("user-service", "learning-service", "assessment-service")) {
    Push-Location (Join-Path "services" $service)
    try {
        mvn -B -DskipTests package
    }
    finally {
        Pop-Location
    }
}

docker compose -f $ComposeFile up --build -d
docker compose -f $ComposeFile ps
