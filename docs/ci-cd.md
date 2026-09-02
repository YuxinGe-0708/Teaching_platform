# CI/CD

The pipeline is defined in `.github/workflows/ci-cd.yml`.

- Pull requests test and build the stateless web BFF, run each business service's unit/API tests, build all microservice images, start `docker-compose.microservices.yml` plus `docker-compose.unified.yml`, and run the microservice smoke and complete business regression scripts.
- When the `AI_API_KEY` secret is configured, the same environment also runs the existing Qwen text and vision smoke test; without that secret, core business CI remains deterministic and the AI test is skipped.
- A push to `dev_dockerfile` (or the configured Codex branch) publishes versioned gateway, web-bff, user-service, learning-service and assessment-service images. Jenkins deploys those exact images to the persistent Docker Desktop Kubernetes cluster on the labeled local host.
- A `v1.2.3` tag uses the same versioned-image flow. Deployment is intentionally handled by Jenkins on the persistent local Docker Desktop cluster rather than by a GitHub-hosted temporary cluster.
- Every deploy job has `needs` on the preceding job; a failed test or image build prevents deployment.
- Compose and Kubernetes logs, pod state, events, and Maven reports are uploaded as GitHub Actions artifacts. GitHub retains successful and failed workflow run records.

Configure GitHub repository secrets `SWR_REGISTRY`, `SWR_ORG`, `SWR_USERNAME`, and `SWR_PASSWORD` for the CI image-publish job. Add `AI_API_KEY` when enabled. The local Jenkins deployment uses its Jenkins credentials and the Docker Desktop kubeconfig; it does not require `KUBE_CONFIG_B64`, CCE, EIP, ELB, or cloud disks.

`k8s-deploy.sh` applies the namespace, secrets, three service schemas/seeds, MySQL, three business services, stateless web-bff and gateway. It never creates the former `teaching_platform` schema. In local mode it validates Docker Desktop Kubernetes, applies the immutable images to the persistent `docker-desktop` context, exposes the gateway on NodePort 30080, waits for every rollout, and checks `/healthz`. Any failure exits non-zero.

The deployment artifacts retain Kubernetes resources, PVCs, events, pod descriptions, container logs, the health response, HTTP headers, and rendered login HTML for 30 days. A future persistent production deployment still needs CCE (or another cluster), a production StorageClass, RDS/managed MySQL, durable upload storage, and protected environments.

## Jenkins deployment

`Jenkinsfile` provides the gated local flow for Jenkins: checkout, build/test, Compose integration regression, versioned image publishing, deployment to the persistent Docker Desktop Kubernetes cluster on the Jenkins host, and health verification. The node must have label `local-docker-desktop`; the pipeline selects the `docker-desktop` context and exposes the gateway at `http://localhost:30080`. Jenkins stops at the first failed stage, while the `post` blocks still archive Compose/Kubernetes logs and test reports.

Required Jenkins agent tools:

- Docker with Compose plugin
- Maven and Java 8
- PowerShell Core (`pwsh`)
- `kubectl`
- Git Bash (or another shell providing `bash`, `curl`, and `envsubst`)

Required Jenkins credentials:

| Credential ID | Type | Purpose |
|---|---|---|
| `swr-registry` | Secret text | Container registry host |
| `swr-org` | Secret text | Registry organization/namespace |
| `swr-account` | Username with password | Registry login |
| `db-root-password` | Secret text | Kubernetes MySQL root password |

For a classroom/demo deployment, leave `DEPLOY_TARGET=local` (the default). Start Docker Desktop with Kubernetes enabled on the Jenkins host. The pipeline pushes immutable `sha-<commit>` images, deploys those exact tags through `scripts/k8s-deploy.sh` to the persistent `docker-desktop` context, checks `http://localhost:30080/healthz`, and leaves the deployment running so refreshing `/login` shows the pushed page changes. `DEPLOY_TARGET=cloud` remains available only when a separate cloud kubeconfig and matching credentials are configured.
