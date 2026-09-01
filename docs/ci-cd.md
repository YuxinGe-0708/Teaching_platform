# CI/CD

The pipeline is defined in `.github/workflows/ci-cd.yml`.

- Pull requests test and build the stateless web BFF, run each business service's unit/API tests, build all microservice images, start `docker-compose.microservices.yml` plus `docker-compose.unified.yml`, and run the microservice smoke and complete business regression scripts.
- When the `AI_API_KEY` secret is configured, the same environment also runs the existing Qwen text and vision smoke test; without that secret, core business CI remains deterministic and the AI test is skipped.
- A push to `dev_dockerfile` (or the configured Codex branch) publishes versioned gateway, web-bff, user-service, learning-service and assessment-service images, creates a temporary kind cluster, deploys those exact images, and repeats health plus complete business regression.
- A `v1.2.3` tag uses the same versioned-image and temporary-kind demonstration flow. The kind cluster is deleted with the GitHub runner after the job, so this is evidence of Kubernetes deployment rather than a persistent production environment.
- Every deploy job has `needs` on the preceding job; a failed test or image build prevents deployment.
- Compose and Kubernetes logs, pod state, events, and Maven reports are uploaded as GitHub Actions artifacts. GitHub retains successful and failed workflow run records.

Configure GitHub repository secrets `SWR_REGISTRY`, `SWR_ORG`, `SWR_USERNAME`, and `SWR_PASSWORD`. Add `AI_API_KEY` when enabled. The temporary deployment uses disposable database passwords and does not require `KUBE_CONFIG_B64`, CCE, EIP, ELB, or cloud disks.

`k8s-deploy.sh` applies the namespace, secrets, three service schemas/seeds, MySQL, three business services, stateless web-bff and gateway. It never creates the former `teaching_platform` schema. In kind mode it maps the gateway to port 3000, waits for every rollout, forwards the three internal service ports, and runs the same API/page business regression used by Compose CI. Any failure exits non-zero.

The deployment artifacts retain Kubernetes resources, PVCs, events, pod descriptions, container logs, the health response, HTTP headers, and rendered login HTML for 30 days. A future persistent production deployment still needs CCE (or another cluster), a production StorageClass, RDS/managed MySQL, durable upload storage, and protected environments.
