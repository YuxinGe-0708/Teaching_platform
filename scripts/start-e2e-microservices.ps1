param(
    [string]$ComposeFile = "docker-compose.microservices.yml"
)

$ErrorActionPreference = "Stop"

docker compose -f $ComposeFile up --build -d
docker compose -f $ComposeFile ps
