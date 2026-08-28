# CI/CD

The pipeline is defined in `.github/workflows/ci-cd.yml`.

- Pull requests run Maven unit tests, build both images, start a clean MySQL/Backend/Frontend Compose environment, and run the container smoke and business regression scripts.
- When the `AI_API_KEY` secret is configured, the same environment also runs the existing Qwen text and vision smoke test; without that secret, core business CI remains deterministic and the AI test is skipped.
- A push to `dev_dockerfile` (or the current working branch `codex/dev-dockerfile-work`) runs the same checks, publishes SHA/versioned images to Huawei Cloud SWR, creates a temporary kind Kubernetes cluster, deploys the published images, and verifies health plus the login/session flow.
- A `v1.2.3` tag uses the same versioned-image and temporary-kind demonstration flow. The kind cluster is deleted with the GitHub runner after the job, so this is evidence of Kubernetes deployment rather than a persistent production environment.
- Every deploy job has `needs` on the preceding job; a failed test or image build prevents deployment.
- Compose and Kubernetes logs, pod state, events, and Maven reports are uploaded as GitHub Actions artifacts. GitHub retains successful and failed workflow run records.

Configure GitHub repository secrets `SWR_REGISTRY`, `SWR_ORG`, `SWR_USERNAME`, and `SWR_PASSWORD`. Add `AI_API_KEY` when enabled. The temporary deployment uses disposable database passwords and does not require `KUBE_CONFIG_B64`, CCE, EIP, ELB, or cloud disks.

`k8s-deploy.sh` applies the namespace, ConfigMap, Secret, MySQL PVC/Deployment, backend PVC/Deployment, and frontend Service. In kind mode it enables repeatable test data, maps the frontend NodePort to runner port 3000, waits for every rollout, checks `/healthz`, renders `/login`, and runs the existing login/session smoke test. Any failure exits non-zero.

The deployment artifacts retain Kubernetes resources, PVCs, events, pod descriptions, container logs, the health response, HTTP headers, and rendered login HTML for 30 days. A future persistent production deployment still needs CCE (or another cluster), a production StorageClass, RDS/managed MySQL, durable upload storage, protected environments, and database migrations.
