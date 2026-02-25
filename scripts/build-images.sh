#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MASTER_IMAGE="${MASTER_IMAGE:-obsdl/master:latest}"
WORKER_IMAGE="${WORKER_IMAGE:-obsdl/worker:latest}"
BASE_IMAGE="${BASE_IMAGE:-eclipse-temurin:17-jre}"
MASTER_BASE_IMAGE="${MASTER_BASE_IMAGE:-$BASE_IMAGE}"
WORKER_BASE_IMAGE="${WORKER_BASE_IMAGE:-$BASE_IMAGE}"

printf '[1/3] Building jars...\n'
mvn -q -DskipTests package

printf '[2/3] Building image %s (base=%s)...\n' "$MASTER_IMAGE" "$MASTER_BASE_IMAGE"
docker build \
  --build-arg BASE_IMAGE="$MASTER_BASE_IMAGE" \
  -f scripts/docker/master.Dockerfile \
  -t "$MASTER_IMAGE" \
  .

printf '[3/3] Building image %s (base=%s)...\n' "$WORKER_IMAGE" "$WORKER_BASE_IMAGE"
docker build \
  --build-arg BASE_IMAGE="$WORKER_BASE_IMAGE" \
  -f scripts/docker/worker.Dockerfile \
  -t "$WORKER_IMAGE" \
  .

printf 'Done.\nMaster image: %s\nWorker image: %s\n' "$MASTER_IMAGE" "$WORKER_IMAGE"
