# CI/CD

The pipeline is defined in `.github/workflows/ci-cd.yml`.

- Pull requests run Maven unit tests, build both images, start a clean MySQL/Backend/Frontend Compose environment, and run the container smoke and business regression scripts.
- When the `AI_API_KEY` secret is configured, the same environment also runs the existing Qwen text and vision smoke test; without that secret, core business CI remains deterministic and the AI test is skipped.
- A push to `dev_dockerfile` (or the current working branch `codex/dev-dockerfile-work`) runs the same checks, publishes SHA/versioned images to Huawei Cloud SWR, and deploys them to the `staging` Kubernetes environment.
- A `v1.2.3` tag uses the same immutable image version and deploys to the protected `production` environment after its required approval.
- Every deploy job has `needs` on the preceding job; a failed test or image build prevents deployment.
- Compose and Kubernetes logs, pod state, events, and Maven reports are uploaded as GitHub Actions artifacts. GitHub retains successful and failed workflow run records.

Configure GitHub secrets `SWR_REGISTRY`, `SWR_ORG`, `SWR_USERNAME`, `SWR_PASSWORD`, `KUBE_CONFIG_B64`, `DB_PASSWORD`, and `DB_ROOT_PASSWORD`. Add `AI_API_KEY` and `JUDGE0_API_KEY` when enabled. Use separate `staging` and `production` environments; require approval for production.

`k8s-deploy.sh` applies the namespace, ConfigMap, Secret, MySQL PVC/Deployment, backend PVC/Deployment, and frontend LoadBalancer. It mounts only `db/init/01_schema.sql`; test data is never inserted automatically into a production cluster. It waits for each rollout and checks `/healthz`; any failure exits non-zero. Run it with `bash scripts/k8s-deploy.sh`. `k8s-rollback.sh` accepts full image references in `BACKEND_IMAGE` and `FRONTEND_IMAGE` and waits for both rollouts.

The Kubernetes MySQL and uploads volumes need a production StorageClass. For production, prefer Huawei Cloud RDS for MySQL and OBS for uploads; these manifests preserve the current self-contained container behavior. Schema changes should be ordered migration scripts before production automation because `db/init` only initializes an empty MySQL volume.
