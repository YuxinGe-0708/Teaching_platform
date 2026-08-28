#!/usr/bin/env bash
set -Eeuo pipefail

namespace="${K8S_NAMESPACE:-teaching-platform}"
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
export AI_API_KEY="${AI_API_KEY:-}"
export JUDGE0_API_KEY="${JUDGE0_API_KEY:-}"
trap 'kubectl -n "$namespace" get pods -o wide > "$artifact_dir/pods.txt" 2>&1 || true; kubectl -n "$namespace" get events --sort-by=.lastTimestamp > "$artifact_dir/events.txt" 2>&1 || true; kubectl -n "$namespace" logs deployment/backend --all-containers --tail=300 > "$artifact_dir/backend.log" 2>&1 || true; kubectl -n "$namespace" logs deployment/frontend --all-containers --tail=100 > "$artifact_dir/frontend.log" 2>&1 || true; rm -f "$secret_file"' EXIT

envsubst < k8s/secret.template.yaml > "$secret_file"
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f "$secret_file"
kubectl -n "$namespace" create secret docker-registry swr-registry \
  --docker-server="$SWR_REGISTRY" --docker-username="$SWR_USERNAME" --docker-password="$SWR_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$namespace" create configmap teaching-platform-db-init --from-file=01_schema.sql=db/init/01_schema.sql --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/mysql.yaml
kubectl -n "$namespace" rollout status deployment/mysql --timeout=5m

backend_file="${RUNNER_TEMP:-/tmp}/backend.yaml"
frontend_file="${RUNNER_TEMP:-/tmp}/frontend.yaml"
sed -e "s|__SWR_REGISTRY__|$SWR_REGISTRY|g" -e "s|__SWR_ORG__|$SWR_ORG|g" -e "s|__IMAGE_TAG__|$IMAGE_TAG|g" k8s/backend.yaml > "$backend_file"
sed -e "s|__SWR_REGISTRY__|$SWR_REGISTRY|g" -e "s|__SWR_ORG__|$SWR_ORG|g" -e "s|__IMAGE_TAG__|$IMAGE_TAG|g" k8s/frontend.yaml > "$frontend_file"
kubectl apply -f "$backend_file"
kubectl apply -f "$frontend_file"
kubectl -n "$namespace" rollout status deployment/backend --timeout=8m
kubectl -n "$namespace" rollout status deployment/frontend --timeout=5m

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
