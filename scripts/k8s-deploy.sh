#!/usr/bin/env bash
set -Eeuo pipefail

namespace="${K8S_NAMESPACE:-teaching-platform}"
deploy_target="${DEPLOY_TARGET:-local}"
kind_cluster_name="${KIND_CLUSTER_NAME:-teaching-platform-demo}"
mysql_image="${MYSQL_IMAGE:-mysql:8.0.43}"
artifact_dir="deploy-artifacts"
mkdir -p "$artifact_dir"
exec > >(tee "$artifact_dir/deploy.log") 2>&1

required=(IMAGE_TAG SWR_REGISTRY SWR_ORG SWR_USERNAME SWR_PASSWORD DB_ROOT_PASSWORD)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required deployment variable: $name" >&2
    exit 1
  fi
done

secret_file="${RUNNER_TEMP:-/tmp}/teaching-platform-secret.yaml"
gateway_file="${RUNNER_TEMP:-/tmp}/gateway.yaml"
bff_file="${RUNNER_TEMP:-/tmp}/web-bff.yaml"
port_forward_pids=()
export AI_API_KEY="${AI_API_KEY:-}"
export JUDGE0_API_KEY="${JUDGE0_API_KEY:-}"
export INTERNAL_API_KEY="${INTERNAL_API_KEY:-dev-internal-key}"

retry() {
  local attempts="$1"
  local delay="$2"
  shift 2
  local attempt
  for attempt in $(seq 1 "$attempts"); do
    if "$@"; then
      return 0
    fi
    echo "Command failed on attempt $attempt/$attempts: $*" >&2
    if [[ "$attempt" == "$attempts" ]]; then
      return 1
    fi
    sleep "$delay"
  done
}

prepare_kind_runtime() {
  if ! command -v kind >/dev/null 2>&1; then
    echo "kind command is required when DEPLOY_TARGET=kind" >&2
    exit 1
  fi
  if ! command -v docker >/dev/null 2>&1; then
    echo "docker command is required when DEPLOY_TARGET=kind" >&2
    exit 1
  fi

  echo "Preloading MySQL image into kind cluster: $kind_cluster_name"
  retry 3 10 docker pull "$mysql_image"
  if [[ "$mysql_image" != "mysql:8.0.43" ]]; then
    docker tag "$mysql_image" mysql:8.0.43
  fi
  retry 3 10 kind load docker-image mysql:8.0.43 --name "$kind_cluster_name"

  local local_images=(
    "teaching-platform-web-bff:$IMAGE_TAG"
    "teaching-platform-gateway:$IMAGE_TAG"
    "teaching-platform-user-service:$IMAGE_TAG"
    "teaching-platform-learning-service:$IMAGE_TAG"
    "teaching-platform-assessment-service:$IMAGE_TAG"
  )
  local image repository remote_image
  for image in "${local_images[@]}"; do
    repository="${image%%:*}"
    remote_image="$SWR_REGISTRY/$SWR_ORG/$repository:$IMAGE_TAG"
    if ! docker image inspect "$image" >/dev/null 2>&1; then
      echo "Local image $image is missing; pulling published image $remote_image"
      echo "$SWR_PASSWORD" | docker login "$SWR_REGISTRY" \
        --username "$SWR_USERNAME" \
        --password-stdin
      retry 3 20 docker pull "$remote_image"
      docker tag "$remote_image" "$image"
    fi
    docker tag "$image" "$remote_image"
    retry 3 10 kind load docker-image "$remote_image" --name "$kind_cluster_name"
  done
}

prepare_local_runtime() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "docker command is required when DEPLOY_TARGET=local" >&2
    exit 1
  fi
  if ! command -v kubectl >/dev/null 2>&1; then
    echo "kubectl command is required when DEPLOY_TARGET=local" >&2
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    echo "Docker Desktop is not running on the Jenkins host" >&2
    exit 1
  fi
  kubectl config use-context docker-desktop >/dev/null
  if ! kubectl cluster-info >/dev/null 2>&1; then
    echo "Docker Desktop Kubernetes is not available" >&2
    exit 1
  fi
}

collect_artifacts() {
  status=$?
  trap - EXIT
  set +e
  for pid in "${port_forward_pids[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  kubectl -n "$namespace" get all,pvc -o wide > "$artifact_dir/kubernetes-resources.txt" 2>&1 || true
  kubectl -n "$namespace" get events --sort-by=.lastTimestamp > "$artifact_dir/events.txt" 2>&1 || true
  kubectl -n "$namespace" describe pods > "$artifact_dir/pod-descriptions.txt" 2>&1 || true
  for deployment in mysql user-service learning-service assessment-service web-bff gateway; do
    kubectl -n "$namespace" logs "deployment/$deployment" --all-containers --tail=300 \
      > "$artifact_dir/$deployment.log" 2>&1 || true
  done
  rm -f "$secret_file" "$gateway_file" "$bff_file"
  exit "$status"
}
trap collect_artifacts EXIT

if [[ "$deploy_target" == "kind" ]]; then
  prepare_kind_runtime
elif [[ "$deploy_target" == "local" ]]; then
  prepare_local_runtime
elif [[ "$deploy_target" != "cloud" ]]; then
  echo "Unsupported DEPLOY_TARGET: $deploy_target (expected local or cloud)" >&2
  exit 1
fi

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
# Remove resources from the former monolithic deployment. The public gateway
# and web-bff below are the only page-serving path in the microservice stack.
kubectl -n "$namespace" delete deployment/backend deployment/frontend service/backend pvc/uploads-data --ignore-not-found
# Do not disable integrations when a deployment job intentionally omits an
# optional key. Reuse the existing secret value across rolling deployments.
if [[ -z "$AI_API_KEY" ]]; then
  existing_ai_key="$(kubectl -n "$namespace" get secret teaching-platform-secrets -o jsonpath='{.data.AI_API_KEY}' 2>/dev/null || true)"
  if [[ -n "$existing_ai_key" ]]; then
    AI_API_KEY="$(printf '%s' "$existing_ai_key" | base64 --decode)"
  fi
fi
if [[ -z "$JUDGE0_API_KEY" ]]; then
  existing_judge_key="$(kubectl -n "$namespace" get secret teaching-platform-secrets -o jsonpath='{.data.JUDGE0_API_KEY}' 2>/dev/null || true)"
  if [[ -n "$existing_judge_key" ]]; then
    JUDGE0_API_KEY="$(printf '%s' "$existing_judge_key" | base64 --decode)"
  fi
fi
kubectl -n "$namespace" create secret generic teaching-platform-secrets \
  --from-literal=MYSQL_ROOT_PASSWORD="$DB_ROOT_PASSWORD" \
  --from-literal=AI_API_KEY="$AI_API_KEY" \
  --from-literal=JUDGE0_API_KEY="$JUDGE0_API_KEY" \
  --from-literal=INTERNAL_API_KEY="$INTERNAL_API_KEY" \
  --dry-run=client -o yaml > "$secret_file"
kubectl apply -f "$secret_file"
kubectl -n "$namespace" create secret docker-registry swr-registry \
  --docker-server="$SWR_REGISTRY" \
  --docker-username="$SWR_USERNAME" \
  --docker-password="$SWR_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl -n "$namespace" create configmap user-service-schema \
  --from-file=schema-user.sql=services/user-service/src/main/resources/db/schema-user.sql \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap learning-service-schema \
  --from-file=schema-learning.sql=services/learning-service/src/main/resources/db/schema-learning.sql \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap assessment-service-schema \
  --from-file=schema-assessment.sql=services/assessment-service/src/main/resources/db/schema-assessment.sql \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap user-service-seed \
  --from-file=seed-user.sql=services/user-service/src/main/resources/db/seed-user.sql \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap learning-service-seed \
  --from-file=seed-learning.sql=services/learning-service/src/main/resources/db/seed-learning.sql \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap assessment-service-seed \
  --from-file=seed-assessment.sql=services/assessment-service/src/main/resources/db/seed-assessment.sql \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f k8s/mysql.yaml
kubectl -n "$namespace" rollout status deployment/mysql --timeout=8m

# The MySQL entrypoint only runs init scripts for a brand-new data directory.
# Re-apply the idempotent schemas and seed data so an existing PVC also gets
# newly introduced service databases and demo accounts.
mysql_pod="$(kubectl -n "$namespace" get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}')"
mysql_scripts=(
  /docker-entrypoint-initdb.d/10-user-service.sql
  /docker-entrypoint-initdb.d/11-learning-service.sql
  /docker-entrypoint-initdb.d/12-assessment-service.sql
  /docker-entrypoint-initdb.d/20-user-seed.sql
  /docker-entrypoint-initdb.d/21-learning-seed.sql
  /docker-entrypoint-initdb.d/22-assessment-seed.sql
)
for mysql_script in "${mysql_scripts[@]}"; do
  echo "Applying idempotent database migration: $mysql_script"
  # Some MySQL 8 versions report duplicate-index errors for CREATE INDEX when
  # a persistent database already has the index. Continue the batch and verify
  # the required schemas/tables below instead of failing on that benign case.
  kubectl -n "$namespace" exec "$mysql_pod" -- mysql --force -uroot "-p$DB_ROOT_PASSWORD" \
    -e "SOURCE $mysql_script" || true
done

schema_check="SELECT IF(
  (SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='user_db')=1 AND
  (SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='learning_service_db')=1 AND
  (SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='assessment_db')=1 AND
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='user_db' AND table_name IN ('user','notification','operation_log'))=3 AND
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='learning_service_db' AND table_name IN ('course','course_enrollment','resource','study_note','discussion_post','discussion_reply'))=6 AND
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='assessment_db' AND table_name IN ('task','submission','exam_record'))=3,
  'ok', 'bad') AS migration_status;"
if ! kubectl -n "$namespace" exec "$mysql_pod" -- mysql -N -uroot "-p$DB_ROOT_PASSWORD" -e "$schema_check" | grep -qx 'ok'; then
  echo "Database migration verification failed" >&2
  exit 1
fi

service_files=(
  services/user-service/k8s/user-service/deployment.yaml
  services/learning-service/k8s/learning-service/deployment.yaml
  services/assessment-service/k8s/assessment-service/deployment.yaml
)
for service_file in "${service_files[@]}"; do
  service_name="$(basename "$(dirname "$service_file")")"
  rendered_file="${RUNNER_TEMP:-/tmp}/$service_name.yaml"
  sed \
    -e "s|__SWR_REGISTRY__|$SWR_REGISTRY|g" \
    -e "s|__SWR_ORG__|$SWR_ORG|g" \
    -e "s|__IMAGE_TAG__|$IMAGE_TAG|g" \
    "$service_file" > "$rendered_file"
  kubectl apply -f "$rendered_file"
done

sed \
  -e "s|__SWR_REGISTRY__|$SWR_REGISTRY|g" \
  -e "s|__SWR_ORG__|$SWR_ORG|g" \
  -e "s|__IMAGE_TAG__|$IMAGE_TAG|g" \
  k8s/backend.yaml > "$bff_file"
kubectl apply -f "$bff_file"

sed \
  -e "s|__SWR_REGISTRY__|$SWR_REGISTRY|g" \
  -e "s|__SWR_ORG__|$SWR_ORG|g" \
  -e "s|__IMAGE_TAG__|$IMAGE_TAG|g" \
  k8s/frontend.yaml > "$gateway_file"
kubectl apply -f "$gateway_file"

if [[ "$deploy_target" == "kind" ]]; then
  kubectl -n "$namespace" set env deployment/assessment-service \
    JUDGE0_API_URL=http://127.0.0.1:9 \
    JUDGE0_TIMEOUT_MS=1000 \
    JUDGE0_LOCAL_FALLBACK=true
  kubectl -n "$namespace" patch service frontend --type=merge \
    -p '{"spec":{"type":"NodePort","ports":[{"port":80,"targetPort":80,"nodePort":30080}]}}'
elif [[ "$deploy_target" == "local" ]]; then
  # Docker Desktop exposes the stable demo URL through this NodePort.
  kubectl -n "$namespace" patch service frontend --type=merge \
    -p '{"spec":{"type":"NodePort","ports":[{"port":80,"targetPort":80,"nodePort":30080}]}}'
fi

for deployment in user-service learning-service assessment-service web-bff gateway; do
  if ! kubectl -n "$namespace" rollout status "deployment/$deployment" --timeout=8m; then
    echo "Rollout failed for deployment/$deployment" >&2
    kubectl -n "$namespace" get pods -o wide >&2 || true
    kubectl -n "$namespace" describe "deployment/$deployment" >&2 || true
    kubectl -n "$namespace" describe pods -l "app=$deployment" >&2 || true
    exit 1
  fi
done
kubectl -n "$namespace" get all,pvc -o wide | tee "$artifact_dir/kubernetes-resources.txt"

if [[ "$deploy_target" == "kind" ]]; then
  kubectl -n "$namespace" port-forward service/frontend 13000:80 \
    > "$artifact_dir/frontend-port-forward.log" 2>&1 &
  port_forward_pids+=("$!")

  frontend_url="http://127.0.0.1:13000"
  frontend_forward_ready=false
  for attempt in {1..60}; do
    if curl --fail --silent --show-error "$frontend_url/healthz" \
      | tee "$artifact_dir/health-response.txt" | grep -qx 'ok'; then
      frontend_forward_ready=true
      break
    fi
    sleep 2
  done
  if [[ "$frontend_forward_ready" != "true" ]]; then
    echo "kind frontend health check failed: $frontend_url/healthz" >&2
    cat "$artifact_dir/frontend-port-forward.log" >&2 || true
    exit 1
  fi
  echo "kind Kubernetes deployment and health check passed: $frontend_url"
  exit 0
fi

if [[ "$deploy_target" == "local" ]]; then
  frontend_url="http://127.0.0.1:30080"
  for attempt in {1..60}; do
    if curl --fail --silent --show-error "$frontend_url/healthz" \
      | tee "$artifact_dir/health-response.txt" | grep -qx 'ok'; then
      echo "Docker Desktop Kubernetes deployment and health check passed: $frontend_url"
      exit 0
    fi
    sleep 2
  done
  echo "Docker Desktop Kubernetes health check failed: $frontend_url/healthz" >&2
  exit 1
fi

frontend_address=""
for attempt in {1..60}; do
  frontend_address="$(kubectl -n "$namespace" get service frontend -o jsonpath='{.status.loadBalancer.ingress[].ip}' 2>/dev/null || true)"
  if [[ -z "$frontend_address" ]]; then
    frontend_address="$(kubectl -n "$namespace" get service frontend -o jsonpath='{.status.loadBalancer.ingress[].hostname}' 2>/dev/null || true)"
  fi
  [[ -n "$frontend_address" ]] && break
  sleep 5
done
if [[ -z "$frontend_address" ]]; then
  echo "Frontend LoadBalancer has no external address yet" >&2
  exit 1
fi
for attempt in {1..60}; do
  if curl --fail --silent --show-error "http://$frontend_address/healthz" | grep -qx 'ok'; then
    echo "Kubernetes deployment and health check passed: $frontend_address"
    exit 0
  fi
  sleep 5
done
echo "Frontend health check failed: http://$frontend_address/healthz" >&2
exit 1
