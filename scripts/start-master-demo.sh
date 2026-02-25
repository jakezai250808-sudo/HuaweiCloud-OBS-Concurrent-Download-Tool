#!/usr/bin/env bash
set -euo pipefail

MASTER_IMAGE="${MASTER_IMAGE:-obsdl/master:latest}"
MASTER_CONTAINER="${MASTER_CONTAINER:-obsdl-master-demo}"
MASTER_PORT="${MASTER_PORT:-8080}"

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
