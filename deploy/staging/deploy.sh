#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: deploy.sh <immutable-ecr-image> <aws-region>" >&2
  exit 64
fi

readonly app_image="$1"
readonly aws_region="$2"
readonly deployment_directory="${NITROS_GAMES_DEPLOYMENT_DIRECTORY:-/opt/nitros-games}"
readonly environment_file="$deployment_directory/.env"
readonly compose_file="$deployment_directory/compose.yaml"
readonly immutable_image_pattern='^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com/[a-z0-9._/-]+:[0-9a-f]{40}$'

if [[ ! "$app_image" =~ $immutable_image_pattern ]]; then
  echo "The application image must be an ECR URI tagged with a full commit SHA." >&2
  exit 65
fi

if [[ ! "$aws_region" =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]]; then
  echo "The AWS region is invalid." >&2
  exit 65
fi

if [[ ! -f "$compose_file" ]]; then
  echo "Missing $compose_file." >&2
  exit 66
fi

umask 077
if [[ ! -f "$environment_file" ]]; then
  db_password="$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')"
  db_root_password="$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')"
  cat > "$environment_file" <<EOF
DB_USERNAME=nitros
DB_PASSWORD=$db_password
DB_ROOT_PASSWORD=$db_root_password
APP_SECURITY_ALLOWED_ORIGINS=http://localhost:4200
OAUTH2_ISSUER_URI=https://identity.invalid/realms/nitros-games
OAUTH2_JWK_SET_URI=https://identity.invalid/realms/nitros-games/protocol/openid-connect/certs
OAUTH2_AUDIENCE=nitros-games-api
EOF
fi

set_app_image() {
  local image="$1"
  local temporary_environment

  temporary_environment="$(mktemp "$deployment_directory/.env.XXXXXX")"
  grep -v '^APP_IMAGE=' "$environment_file" > "$temporary_environment" || true
  printf 'APP_IMAGE=%s\n' "$image" >> "$temporary_environment"
  chmod 0600 "$temporary_environment"
  mv "$temporary_environment" "$environment_file"
}

compose_with_environment() {
  local selected_environment="$1"
  shift
  docker compose --env-file "$selected_environment" --file "$compose_file" "$@"
}

start_and_verify() {
  compose_with_environment "$environment_file" config --quiet || return $?
  compose_with_environment "$environment_file" up \
    --detach --remove-orphans --wait --wait-timeout 240 || return $?
  curl --fail --silent --show-error \
    http://127.0.0.1:8080/actuator/health/readiness || return $?
  echo
}

previous_image="$(sed -n 's/^APP_IMAGE=//p' "$environment_file" | tail -n 1)"
if [[ -n "$previous_image" && ! "$previous_image" =~ $immutable_image_pattern ]]; then
  echo "The currently configured APP_IMAGE is not an immutable ECR image; deployment aborted." >&2
  exit 67
fi

registry="${app_image%%/*}"
if ! aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$registry"; then
  echo "DEPLOYMENT_RESULT=unchanged"
  echo "ACTIVE_IMAGE=${previous_image:-unknown}"
  exit 68
fi

cd "$deployment_directory"
candidate_environment="$(mktemp "$deployment_directory/.env.XXXXXX")"
trap '[[ -z "${candidate_environment:-}" || ! -f "$candidate_environment" ]] || rm -f "$candidate_environment"' EXIT
grep -v '^APP_IMAGE=' "$environment_file" > "$candidate_environment" || true
printf 'APP_IMAGE=%s\n' "$app_image" >> "$candidate_environment"
chmod 0600 "$candidate_environment"

# Validate and pull before changing the live environment. A registry or image
# failure therefore leaves the currently running deployment untouched.
if ! compose_with_environment "$candidate_environment" config --quiet \
  || ! compose_with_environment "$candidate_environment" pull; then
  echo "DEPLOYMENT_RESULT=unchanged"
  echo "FAILED_IMAGE=$app_image"
  echo "ACTIVE_IMAGE=${previous_image:-unknown}"
  exit 69
fi

mv "$candidate_environment" "$environment_file"
candidate_environment=""

deployment_status=0
start_and_verify || deployment_status=$?
if [[ $deployment_status -eq 0 ]]; then
  echo "DEPLOYMENT_RESULT=success"
  echo "DEPLOYED_IMAGE=$app_image"
  echo "PREVIOUS_IMAGE=${previous_image:-none}"
  docker image prune --force
  compose_with_environment "$environment_file" ps
  exit 0
fi

echo "Candidate image failed readiness with status $deployment_status." >&2
if [[ -n "$previous_image" && "$previous_image" != "$app_image" ]]; then
  echo "Restoring previous image $previous_image." >&2
  set_app_image "$previous_image"

  rollback_status=0
  start_and_verify || rollback_status=$?
  if [[ $rollback_status -eq 0 ]]; then
    echo "DEPLOYMENT_RESULT=rolled_back"
    echo "FAILED_IMAGE=$app_image"
    echo "RESTORED_IMAGE=$previous_image"
    compose_with_environment "$environment_file" ps
    exit 70
  fi

  echo "DEPLOYMENT_RESULT=rollback_failed"
  echo "FAILED_IMAGE=$app_image"
  echo "ROLLBACK_IMAGE=$previous_image"
  compose_with_environment "$environment_file" ps || true
  exit 71
fi

echo "DEPLOYMENT_RESULT=rollback_unavailable"
echo "FAILED_IMAGE=$app_image"
echo "PREVIOUS_IMAGE=${previous_image:-none}"
compose_with_environment "$environment_file" ps || true
exit 72
