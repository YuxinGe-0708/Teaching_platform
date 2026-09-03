# CI/CD

The pipeline is defined in `.github/workflows/ci-cd.yml`.

- Pull requests test and build the stateless web BFF, run each business service's unit/API tests, build all microservice images, start `docker-compose.microservices.yml` plus `docker-compose.unified.yml`, and run the microservice smoke and complete business regression scripts.
- When the `AI_API_KEY` secret is configured, the same environment also runs the existing Qwen text and vision smoke test; without that secret, core business CI remains deterministic and the AI test is skipped.
- A push to `dev_dockerfile` (or the configured Codex branch) publishes versioned gateway, web-bff, user-service, learning-service and assessment-service images, creates a temporary kind cluster, deploys those exact images, and repeats health plus complete business regression.
- A `v1.2.3` tag uses the same versioned-image and temporary-kind demonstration flow. The kind cluster is deleted with the GitHub runner after the job, so this is evidence of Kubernetes deployment rather than a persistent production environment.
- Every deploy job has `needs` on the preceding job; a failed test or image build prevents deployment.
- Compose and Kubernetes logs, pod state, events, and Maven reports are uploaded as GitHub Actions artifacts. GitHub retains successful and failed workflow run records.

Configure GitHub repository secrets `SWR_REGISTRY`, `SWR_ORG`, `SWR_USERNAME`, and `SWR_PASSWORD`. Add `AI_API_KEY` when enabled. The temporary deployment uses disposable database passwords and does not require `KUBE_CONFIG_B64`, CCE, EIP, ELB, or cloud disks.

`k8s-deploy.sh` applies the namespace, secrets, three service schemas/seeds, MySQL, three business services, stateless web-bff and gateway. It never creates the former `teaching_platform` schema. In kind mode it preloads the published immutable images into the temporary cluster, waits for every rollout, forwards the gateway to local port 13000 plus the three internal service ports, and runs the same API/page business regression used by Compose CI. Any failure exits non-zero.

The deployment artifacts retain Kubernetes resources, PVCs, events, pod descriptions, container logs, the health response, HTTP headers, and rendered login HTML for 30 days. A future persistent production deployment still needs CCE (or another cluster), a production StorageClass, RDS/managed MySQL, durable upload storage, and protected environments.

## Jenkins deployment

`Jenkinsfile` provides the same gated flow for Jenkins: checkout, build/test, Compose integration regression, versioned image publishing, Kubernetes deployment, and health/regression verification. Jenkins stops at the first failed stage, while the `post` blocks still archive Compose/Kubernetes logs and test reports.

Required Jenkins agent tools:

- Docker with Compose plugin
- Maven and Java 8
- PowerShell Core (`pwsh`)
- `kubectl`
- `kind` when `DEPLOY_TARGET=kind`

Required Jenkins credentials:

| Credential ID | Type | Purpose |
|---|---|---|
| `swr-registry` | Secret text | Container registry host |
| `swr-org` | Secret text | Registry organization/namespace |
| `swr-account` | Username with password | Registry login |
| `db-root-password` | Secret text | Kubernetes MySQL root password |

For a classroom/demo deployment, set `DEPLOY_TARGET=kind`. The Jenkins pipeline creates or reuses the named kind cluster, pushes immutable `sha-<commit>` images, deploys those exact tags through `scripts/k8s-deploy.sh`, checks `/healthz`, then runs the microservice smoke and business regression scripts.
