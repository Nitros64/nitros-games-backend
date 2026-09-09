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
readonly keycloak_admin_password_parameter="/nitros-games/staging/keycloak/admin-password"
readonly keycloak_admin_client_secret_parameter="/nitros-games/staging/keycloak/admin-client-secret"
readonly keycloak_reader_client_secret_parameter="/nitros-games/staging/keycloak/reader-client-secret"
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
EOF
fi

read_secure_parameter() {
  local parameter_name="$1"
  local value

  value="$(aws ssm get-parameter \
    --region "$aws_region" \
    --name "$parameter_name" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text)"
  [[ -n "$value" && "$value" != "None" ]] || {
    echo "Secure parameter $parameter_name is empty or unavailable." >&2
    return 1
  }
  printf '%s' "$value"
}

configure_identity() {
  local admin_password admin_client_secret reader_client_secret temporary_environment

  admin_password="$(read_secure_parameter "$keycloak_admin_password_parameter")" || return $?
  admin_client_secret="$(read_secure_parameter "$keycloak_admin_client_secret_parameter")" || return $?
  reader_client_secret="$(read_secure_parameter "$keycloak_reader_client_secret_parameter")" || return $?
  temporary_environment="$(mktemp "$deployment_directory/.env.XXXXXX")"

  grep -Ev \
    '^(KEYCLOAK_ADMIN_USERNAME|KEYCLOAK_ADMIN_PASSWORD|NITROS_GAMES_CLI_SECRET|NITROS_GAMES_READER_CLI_SECRET|OAUTH2_ISSUER_URI|OAUTH2_JWK_SET_URI|OAUTH2_AUDIENCE|OAUTH2_RESOURCE_ID|OAUTH2_ACCESS_SCOPE|OAUTH2_ADMIN_SCOPE|OAUTH2_ALLOWED_CLIENT_IDS)=' \
    "$environment_file" > "$temporary_environment" || true
  {
    printf 'KEYCLOAK_ADMIN_USERNAME=admin\n'
    printf 'KEYCLOAK_ADMIN_PASSWORD=%s\n' "$admin_password"
    printf 'NITROS_GAMES_CLI_SECRET=%s\n' "$admin_client_secret"
    printf 'NITROS_GAMES_READER_CLI_SECRET=%s\n' "$reader_client_secret"
    printf 'OAUTH2_ISSUER_URI=http://localhost:8081/realms/nitros-games\n'
    printf 'OAUTH2_JWK_SET_URI=http://keycloak:8080/realms/nitros-games/protocol/openid-connect/certs\n'
    printf 'OAUTH2_RESOURCE_ID=nitros-games-api\n'
    printf 'OAUTH2_ACCESS_SCOPE=nitros-games-api/access\n'
    printf 'OAUTH2_ADMIN_SCOPE=nitros-games-api/admin\n'
    printf 'OAUTH2_ALLOWED_CLIENT_IDS=nitros-games-web,nitros-games-cli,nitros-games-reader-cli\n'
  } >> "$temporary_environment"
  chmod 0600 "$temporary_environment"
  mv "$temporary_environment" "$environment_file"
}

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

start_dependencies() {
  local selected_environment="$1"
  compose_with_environment "$selected_environment" up \
    --detach --wait --wait-timeout 240 mysql keycloak
}

start_and_verify_api() {
  compose_with_environment "$environment_file" up \
    --detach --no-deps --wait --wait-timeout 240 api || return $?
  curl --fail --silent --show-error \
    http://127.0.0.1:8080/actuator/health/readiness || return $?
  echo
}

if ! configure_identity; then
  echo "DEPLOYMENT_RESULT=unchanged"
  echo "Identity secrets could not be loaded from Parameter Store." >&2
  exit 68
fi

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

# Keycloak and MySQL are staging infrastructure, not application release
# artifacts. Verify them before changing APP_IMAGE so an infrastructure failure
# leaves the currently configured application image untouched.
if ! start_dependencies "$candidate_environment"; then
  echo "DEPLOYMENT_RESULT=unchanged"
  echo "FAILED_IMAGE=$app_image"
  echo "ACTIVE_IMAGE=${previous_image:-unknown}"
  echo "Staging dependencies did not become healthy." >&2
  exit 69
fi

mv "$candidate_environment" "$environment_file"
candidate_environment=""

deployment_status=0
start_and_verify_api || deployment_status=$?
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
  start_and_verify_api || rollback_status=$?
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
