#!/usr/bin/env bash
set -euo pipefail

MASTER_IMAGE="${MASTER_IMAGE:-obsdl/master:latest}"
MASTER_CONTAINER="${MASTER_CONTAINER:-obsdl-master-demo}"
MASTER_PORT="${MASTER_PORT:-8080}"

if ! docker image inspect "$MASTER_IMAGE" >/dev/null 2>&1; then
  echo "Image not found: $MASTER_IMAGE" >&2
  echo "Please build first: ./scripts/build-images.sh" >&2
  exit 1
fi

if ! docker run --rm --entrypoint java "$MASTER_IMAGE" -version >/dev/null 2>&1; then
  echo "Image '$MASTER_IMAGE' does not provide runnable java runtime." >&2
  echo "Rebuild with valid base image, e.g.: BASE_IMAGE=eclipse-temurin:17-jre ./scripts/build-images.sh" >&2
  exit 1
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$MASTER_CONTAINER"; then
  docker rm -f "$MASTER_CONTAINER" >/dev/null
fi

docker run -d \
  --name "$MASTER_CONTAINER" \
  -p "$MASTER_PORT:8080" \
  -e SPRING_PROFILES_ACTIVE=demo \
  "$MASTER_IMAGE" >/dev/null

cat <<MSG
Demo master started: http://localhost:${MASTER_PORT}
H2 Console         : http://localhost:${MASTER_PORT}/h2-console
Stop command       : docker rm -f ${MASTER_CONTAINER}
MSG
