#!/usr/bin/env bash
set -euo pipefail

MASTER_IMAGE="${MASTER_IMAGE:-obsdl/master:latest}"
MASTER_CONTAINER="${MASTER_CONTAINER:-obsdl-master-demo}"
MASTER_PORT="${MASTER_PORT:-8080}"
HOST_BIND="${HOST_BIND:-0.0.0.0}"
ACCESS_HOST="${ACCESS_HOST:-$(hostname -I 2>/dev/null | awk '{print $1}')}"
ACCESS_HOST="${ACCESS_HOST:-localhost}"
STARTUP_WAIT_SECONDS="${STARTUP_WAIT_SECONDS:-10}"

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
  -p "$HOST_BIND:$MASTER_PORT:8080" \
  -e SPRING_PROFILES_ACTIVE=demo \
  "$MASTER_IMAGE" >/dev/null

for _ in $(seq 1 "$STARTUP_WAIT_SECONDS"); do
  status=$(docker inspect -f '{{.State.Status}}' "$MASTER_CONTAINER" 2>/dev/null || echo "unknown")
  case "$status" in
    running)
      echo "Demo master started:"
      echo "- Bind       : ${HOST_BIND}"
      echo "- Master URL : http://${ACCESS_HOST}:${MASTER_PORT}"
      echo "- H2 Console : http://${ACCESS_HOST}:${MASTER_PORT}/h2-console"
      echo "Stop command : docker rm -f ${MASTER_CONTAINER}"
      exit 0
      ;;
    exited|dead)
      echo "Container ${MASTER_CONTAINER} failed to start (status=${status}). Recent logs:" >&2
      docker logs --tail 200 "$MASTER_CONTAINER" >&2 || true
      exit 1
      ;;
  esac
  sleep 1
done

echo "Container ${MASTER_CONTAINER} is not running after ${STARTUP_WAIT_SECONDS}s. Recent logs:" >&2
docker logs --tail 200 "$MASTER_CONTAINER" >&2 || true
exit 1
