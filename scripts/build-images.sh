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
# - auto (default): build/repackage jars only when missing or non-executable
# - always: always run maven package + spring-boot:repackage
# - never: never run maven (require existing executable Spring Boot jars)
BUILD_JAR="${BUILD_JAR:-auto}"

check_java_runtime() {
  local image="$1"
  local label="$2"
  if ! docker run --rm --entrypoint java "$image" -version >/dev/null 2>&1; then
    echo "${label} image does not provide runnable 'java' command: ${image}" >&2
    echo "Please use a Java runtime base image (for example eclipse-temurin:17-jre)." >&2
    exit 1
  fi
}

is_executable_boot_jar() {
  local jar_file="$1"
  unzip -p "$jar_file" META-INF/MANIFEST.MF 2>/dev/null | grep -Eq '^Start-Class: |^Main-Class: org\.springframework\.boot\.loader\.'
}

pick_single_jar() {
  local module="$1"
  local jar
  jar=$(find "$module/target" -maxdepth 1 -type f -name '*.jar' | head -n 1 || true)
  echo "$jar"
}

build_repackage() {
  printf '[1/4] Building executable Spring Boot jars...\n'
  mvn -q -pl master -am -DskipTests package spring-boot:repackage
  mvn -q -pl worker -am -DskipTests package spring-boot:repackage
}

MASTER_JAR=$(pick_single_jar master)
WORKER_JAR=$(pick_single_jar worker)
MASTER_OK=false
WORKER_OK=false

if [[ -n "$MASTER_JAR" ]] && is_executable_boot_jar "$MASTER_JAR"; then
  MASTER_OK=true
fi
if [[ -n "$WORKER_JAR" ]] && is_executable_boot_jar "$WORKER_JAR"; then
  WORKER_OK=true
fi

need_build=false
case "$BUILD_JAR" in
  always)
    need_build=true
    ;;
  never)
    need_build=false
    ;;
  auto)
    if [[ "$MASTER_OK" != true || "$WORKER_OK" != true ]]; then
      need_build=true
    fi
    ;;
  *)
    echo "Invalid BUILD_JAR value: $BUILD_JAR (allowed: auto|always|never)" >&2
    exit 1
    ;;
esac

if [[ "$need_build" == true ]]; then
  build_repackage
else
  printf '[1/4] Skipping Maven build (BUILD_JAR=%s).\n' "$BUILD_JAR"
fi

MASTER_JAR=$(pick_single_jar master)
WORKER_JAR=$(pick_single_jar worker)
if [[ -z "$MASTER_JAR" || -z "$WORKER_JAR" ]]; then
  echo "Missing jar(s): master/target/*.jar or worker/target/*.jar not found." >&2
  echo "Please run BUILD_JAR=always or package jars first." >&2
  exit 1
fi

if ! is_executable_boot_jar "$MASTER_JAR" || ! is_executable_boot_jar "$WORKER_JAR"; then
  echo "Detected non-executable jar (no Spring Boot main manifest)." >&2
  echo "This causes: 'no main manifest attribute, in /app/app.jar'." >&2
  echo "Fix by rebuilding executable jars:" >&2
  echo "  BUILD_JAR=always ./scripts/build-images.sh" >&2
  echo "or run manually on build machine:" >&2
  echo "  mvn -pl master -am -DskipTests package spring-boot:repackage" >&2
  echo "  mvn -pl worker -am -DskipTests package spring-boot:repackage" >&2
  exit 1
fi

printf '[2/4] Validating base images provide Java runtime...\n'
check_java_runtime "$MASTER_BASE_IMAGE" "Master base"
check_java_runtime "$WORKER_BASE_IMAGE" "Worker base"

printf '[3/4] Building image %s (base=%s)...\n' "$MASTER_IMAGE" "$MASTER_BASE_IMAGE"
docker build \
  --build-arg BASE_IMAGE="$MASTER_BASE_IMAGE" \
  -f scripts/docker/master.Dockerfile \
  -t "$MASTER_IMAGE" \
  .

printf '[4/4] Building image %s (base=%s)...\n' "$WORKER_IMAGE" "$WORKER_BASE_IMAGE"
docker build \
  --build-arg BASE_IMAGE="$WORKER_BASE_IMAGE" \
  -f scripts/docker/worker.Dockerfile \
  -t "$WORKER_IMAGE" \
  .

printf 'Verifying Java runtime in built images...\n'
check_java_runtime "$MASTER_IMAGE" "Master built"
check_java_runtime "$WORKER_IMAGE" "Worker built"

printf 'Done.\nMaster image: %s\nWorker image: %s\n' "$MASTER_IMAGE" "$WORKER_IMAGE"
