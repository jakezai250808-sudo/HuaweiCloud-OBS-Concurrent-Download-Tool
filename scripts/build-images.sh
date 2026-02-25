#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MASTER_IMAGE="${MASTER_IMAGE:-obsdl/master:latest}"
WORKER_IMAGE="${WORKER_IMAGE:-obsdl/worker:latest}"
BASE_IMAGE="${BASE_IMAGE:-eclipse-temurin:17-jre}"
MASTER_BASE_IMAGE="${MASTER_BASE_IMAGE:-$BASE_IMAGE}"
WORKER_BASE_IMAGE="${WORKER_BASE_IMAGE:-$BASE_IMAGE}"

# BUILD_JAR modes:
# - auto (default): build jar only when target jars are missing
# - always: always run maven package
# - never: never run maven package (require existing jars)
BUILD_JAR="${BUILD_JAR:-auto}"

MASTER_JAR_COUNT=$(find master/target -maxdepth 1 -type f -name '*.jar' 2>/dev/null | wc -l | tr -d ' ')
WORKER_JAR_COUNT=$(find worker/target -maxdepth 1 -type f -name '*.jar' 2>/dev/null | wc -l | tr -d ' ')

need_build=false
case "$BUILD_JAR" in
  always)
    need_build=true
    ;;
  never)
    need_build=false
    ;;
  auto)
    if [[ "$MASTER_JAR_COUNT" -eq 0 || "$WORKER_JAR_COUNT" -eq 0 ]]; then
      need_build=true
    fi
    ;;
  *)
    echo "Invalid BUILD_JAR value: $BUILD_JAR (allowed: auto|always|never)" >&2
    exit 1
    ;;
esac

if [[ "$need_build" == true ]]; then
  printf '[1/3] Building jars with Maven...\n'
  mvn -q -DskipTests package
else
  printf '[1/3] Skipping Maven package (BUILD_JAR=%s).\n' "$BUILD_JAR"
fi

MASTER_JAR_COUNT=$(find master/target -maxdepth 1 -type f -name '*.jar' 2>/dev/null | wc -l | tr -d ' ')
WORKER_JAR_COUNT=$(find worker/target -maxdepth 1 -type f -name '*.jar' 2>/dev/null | wc -l | tr -d ' ')
if [[ "$MASTER_JAR_COUNT" -eq 0 || "$WORKER_JAR_COUNT" -eq 0 ]]; then
  echo "Missing jar(s): master/target/*.jar or worker/target/*.jar not found." >&2
  echo "Please run Maven package first or set BUILD_JAR=always." >&2
  exit 1
fi

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
