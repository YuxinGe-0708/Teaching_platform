#!/usr/bin/env bash
set -Eeuo pipefail
namespace="${K8S_NAMESPACE:-teaching-platform}"
backend_image="${BACKEND_IMAGE:?BACKEND_IMAGE is required}"
frontend_image="${FRONTEND_IMAGE:?FRONTEND_IMAGE is required}"
kubectl -n "$namespace" set image deployment/backend backend="$backend_image"
kubectl -n "$namespace" set image deployment/frontend frontend="$frontend_image"
kubectl -n "$namespace" rollout status deployment/backend --timeout=8m
kubectl -n "$namespace" rollout status deployment/frontend --timeout=5m
echo "Rollback completed."
