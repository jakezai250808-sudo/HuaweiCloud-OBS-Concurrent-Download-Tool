#!/usr/bin/env bash
set -euo pipefail

NETWORK_NAME="${NETWORK_NAME:-obsdl-net}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-obsdl-mysql}"
MASTER_CONTAINER="${MASTER_CONTAINER:-obsdl-master}"
WORKER_CONTAINER="${WORKER_CONTAINER:-obsdl-worker}"

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-obsdl}"

MASTER_IMAGE="${MASTER_IMAGE:-obsdl/master:latest}"
WORKER_IMAGE="${WORKER_IMAGE:-obsdl/worker:latest}"

MASTER_PORT="${MASTER_PORT:-8080}"
WORKER_PORT="${WORKER_PORT:-8081}"
MYSQL_PORT="${MYSQL_PORT:-3306}"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  docker network create "$NETWORK_NAME" >/dev/null
fi

if ! docker ps -a --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  docker run -d \
    --name "$MYSQL_CONTAINER" \
    --network "$NETWORK_NAME" \
    -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
    -e MYSQL_DATABASE="$MYSQL_DATABASE" \
    -p "$MYSQL_PORT:3306" \
    -v "$ROOT_DIR/db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro" \
    mysql:8.0 >/dev/null
else
  docker start "$MYSQL_CONTAINER" >/dev/null
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$MASTER_CONTAINER"; then
  docker rm -f "$MASTER_CONTAINER" >/dev/null
fi

docker run -d \
  --name "$MASTER_CONTAINER" \
  --network "$NETWORK_NAME" \
  -p "$MASTER_PORT:8080" \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://$MYSQL_CONTAINER:3306/$MYSQL_DATABASE?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  "$MASTER_IMAGE" >/dev/null

if docker ps -a --format '{{.Names}}' | grep -qx "$WORKER_CONTAINER"; then
  docker rm -f "$WORKER_CONTAINER" >/dev/null
fi

docker run -d \
  --name "$WORKER_CONTAINER" \
  --network "$NETWORK_NAME" \
  -p "$WORKER_PORT:8081" \
  -e MASTER_URL="http://$MASTER_CONTAINER:8080" \
  "$WORKER_IMAGE" >/dev/null

cat <<MSG
Stack started:
- MySQL : localhost:${MYSQL_PORT} (${MYSQL_CONTAINER})
- Master: http://localhost:${MASTER_PORT} (${MASTER_CONTAINER})
- Worker: http://localhost:${WORKER_PORT} (${WORKER_CONTAINER})

Stop command:
  docker rm -f ${WORKER_CONTAINER} ${MASTER_CONTAINER} ${MYSQL_CONTAINER}
MSG
