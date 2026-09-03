#!/usr/bin/env bash
set -Eeuo pipefail
namespace="${K8S_NAMESPACE:-teaching-platform}"
: "${WEB_BFF_IMAGE:?WEB_BFF_IMAGE is required}"
: "${GATEWAY_IMAGE:?GATEWAY_IMAGE is required}"
: "${USER_SERVICE_IMAGE:?USER_SERVICE_IMAGE is required}"
: "${LEARNING_SERVICE_IMAGE:?LEARNING_SERVICE_IMAGE is required}"
: "${ASSESSMENT_SERVICE_IMAGE:?ASSESSMENT_SERVICE_IMAGE is required}"

kubectl -n "$namespace" set image deployment/web-bff web-bff="$WEB_BFF_IMAGE"
kubectl -n "$namespace" set image deployment/gateway gateway="$GATEWAY_IMAGE"
kubectl -n "$namespace" set image deployment/user-service user-service="$USER_SERVICE_IMAGE"
kubectl -n "$namespace" set image deployment/learning-service learning-service="$LEARNING_SERVICE_IMAGE"
kubectl -n "$namespace" set image deployment/assessment-service assessment-service="$ASSESSMENT_SERVICE_IMAGE"
for deployment in user-service learning-service assessment-service web-bff gateway; do
  kubectl -n "$namespace" rollout status "deployment/$deployment" --timeout=8m
done
echo "Microservice rollback completed."
