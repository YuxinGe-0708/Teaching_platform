#!/usr/bin/env bash
set -Eeuo pipefail

namespace="${K8S_NAMESPACE:-teaching-platform}"
deploy_target="${DEPLOY_TARGET:-cloud}"
artifact_dir="deploy-artifacts"
mkdir -p "$artifact_dir"
exec > >(tee "$artifact_dir/deploy.log") 2>&1

required=(IMAGE_TAG SWR_REGISTRY SWR_ORG SWR_USERNAME SWR_PASSWORD DB_PASSWORD DB_ROOT_PASSWORD)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required deployment variable: $name" >&2
    exit 1
  fi
done

secret_file="${RUNNER_TEMP:-/tmp}/teaching-platform-secret.yaml"
port_forward_pid=""
export AI_API_KEY="${AI_API_KEY:-}"
export JUDGE0_API_KEY="${JUDGE0_API_KEY:-}"

collect_artifacts() {
  status=$?
  trap - EXIT
  set +e
  if [[ -n "$port_forward_pid" ]]; then kill "$port_forward_pid" 2>/dev/null || true; fi
  kubectl -n "$namespace" get all,pvc -o wide > "$artifact_dir/kubernetes-resources.txt" 2>&1 || true
  kubectl -n "$namespace" get events --sort-by=.lastTimestamp > "$artifact_dir/events.txt" 2>&1 || true
  kubectl -n "$namespace" describe pods > "$artifact_dir/pod-descriptions.txt" 2>&1 || true
  kubectl -n "$namespace" logs deployment/mysql --all-containers --tail=300 > "$artifact_dir/mysql.log" 2>&1 || true
  kubectl -n "$namespace" logs deployment/backend --all-containers --tail=300 > "$artifact_dir/backend.log" 2>&1 || true
  kubectl -n "$namespace" logs deployment/frontend --all-containers --tail=100 > "$artifact_dir/frontend.log" 2>&1 || true
  rm -f "$secret_file"
  exit "$status"
}
trap collect_artifacts EXIT

envsubst < k8s/secret.template.yaml > "$secret_file"
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f "$secret_file"
kubectl -n "$namespace" create secret docker-registry swr-registry \
  --docker-server="$SWR_REGISTRY" --docker-username="$SWR_USERNAME" --docker-password="$SWR_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap teaching-platform-db-init --from-file=01_schema.sql=db/init/01_schema.sql --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap teaching-platform-db-migrations --from-file=db/migrations --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/mysql.yaml
kubectl -n "$namespace" rollout status deployment/mysql --timeout=5m

# The readiness probe uses TCP. Use the same path here instead of MySQL's
# default Unix socket, which may not exist while the server is initializing.
mysql_exec=(kubectl -n "$namespace" exec deployment/mysql -- env "MYSQL_PWD=$DB_ROOT_PASSWORD" mysql -h 127.0.0.1 -P 3306 --protocol=tcp -uroot)
for attempt in {1..30}; do
  if "${mysql_exec[@]}" teaching_platform -NBe 'SELECT 1' >/dev/null 2>&1; then
    break
  fi
  if [[ "$attempt" == 30 ]]; then
    echo "MySQL TCP connection was not ready before migrations" >&2
    exit 1
  fi
  sleep 2
done

for migration_file in db/migrations/*.sql; do
  migration_version="$(basename "$migration_file" .sql)"
  migration_applied="$("${mysql_exec[@]}" teaching_platform -NBe "SELECT COUNT(*) FROM schema_migrations WHERE version='$migration_version';" 2>/dev/null || true)"
  if [[ "$migration_applied" == "1" ]]; then
    echo "Skipping already applied migration $migration_version"
    continue
  fi
  echo "Applying migration $migration_version"
  kubectl -n "$namespace" exec -i deployment/mysql -- env "MYSQL_PWD=$DB_ROOT_PASSWORD" mysql -h 127.0.0.1 -P 3306 --protocol=tcp -uroot teaching_platform < "$migration_file"
  "${mysql_exec[@]}" -e "INSERT INTO teaching_platform.schema_migrations (version) VALUES ('$migration_version');"
done

backend_file="${RUNNER_TEMP:-/tmp}/backend.yaml"
frontend_file="${RUNNER_TEMP:-/tmp}/frontend.yaml"
sed -e "s|__SWR_REGISTRY__|$SWR_REGISTRY|g" -e "s|__SWR_ORG__|$SWR_ORG|g" -e "s|__IMAGE_TAG__|$IMAGE_TAG|g" k8s/backend.yaml > "$backend_file"
sed -e "s|__SWR_REGISTRY__|$SWR_REGISTRY|g" -e "s|__SWR_ORG__|$SWR_ORG|g" -e "s|__IMAGE_TAG__|$IMAGE_TAG|g" k8s/frontend.yaml > "$frontend_file"
kubectl apply -f "$backend_file"
kubectl apply -f "$frontend_file"
if [[ "$deploy_target" == "kind" ]]; then
  kubectl -n "$namespace" set env deployment/backend APP_SEED_TEST_DATA=true
  kubectl -n "$namespace" patch service frontend --type=merge \
    -p '{"spec":{"type":"NodePort","ports":[{"port":80,"targetPort":80,"nodePort":30080}]}}'
fi
kubectl -n "$namespace" rollout status deployment/backend --timeout=8m
kubectl -n "$namespace" rollout status deployment/frontend --timeout=5m
kubectl -n "$namespace" get all,pvc -o wide | tee "$artifact_dir/kubernetes-resources.txt"

if [[ "$deploy_target" == "kind" ]]; then
  frontend_url="http://127.0.0.1:3000"
  for attempt in {1..30}; do
    if curl --fail --silent --show-error "$frontend_url/healthz" | tee "$artifact_dir/health-response.txt" | grep -qx 'ok'; then
      curl --fail --silent --show-error --dump-header "$artifact_dir/login-headers.txt" \
        --output "$artifact_dir/login-page.html" "$frontend_url/login"
      kubectl -n "$namespace" port-forward service/backend 8081:8080 > "$artifact_dir/backend-port-forward.log" 2>&1 &
      port_forward_pid=$!
      for backend_attempt in {1..20}; do
        curl --fail --silent http://127.0.0.1:8081/login > /dev/null && break
        sleep 1
      done
      pwsh -File scripts/container-smoke.ps1 -BaseUrl "$frontend_url" -BackendUrl http://127.0.0.1:8081
      echo "Ephemeral kind deployment, health check, and login smoke test passed."
      exit 0
    fi
    sleep 5
  done
  echo "kind frontend health check failed: $frontend_url/healthz" >&2
  exit 1
fi

frontend_address=""
for attempt in {1..30}; do
  frontend_address="$(kubectl -n "$namespace" get service frontend -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)"
  if [[ -z "$frontend_address" ]]; then
    frontend_address="$(kubectl -n "$namespace" get service frontend -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)"
  fi
  [[ -n "$frontend_address" ]] && break
  sleep 10
done
if [[ -z "$frontend_address" ]]; then
  echo "Frontend LoadBalancer has no external address yet" >&2
  exit 1
fi
for attempt in {1..30}; do
  if curl --fail --silent --show-error "http://$frontend_address/healthz" | grep -qx 'ok'; then
    echo "Kubernetes deployment and health check passed: $frontend_address"
    exit 0
  fi
  sleep 5
done
echo "Frontend health check failed: http://$frontend_address/healthz" >&2
exit 1
