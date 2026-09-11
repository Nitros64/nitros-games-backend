#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: deploy.sh <immutable-ecr-image> <aws-region>" >&2
  exit 64
fi

readonly app_image="$1"
readonly aws_region="$2"
readonly deployment_directory="${NITROS_GAMES_DEPLOYMENT_DIRECTORY:-/opt/nitros-games}"
readonly runtime_directory="${NITROS_GAMES_RUNTIME_DIRECTORY:-/run/nitros-games}"
readonly configuration_file="$deployment_directory/runtime.conf"
readonly compose_file="$deployment_directory/compose.yaml"
readonly environment_file="$runtime_directory/runtime.env"
readonly active_image_file="$deployment_directory/active-image"
readonly candidate_log_file="$runtime_directory/candidate-failure.log"
readonly immutable_image_pattern="^[0-9]{12}\\.dkr\\.ecr\\.${aws_region}\\.amazonaws\\.com/nitros-games-backend:[0-9a-f]{40}$"

if [[ ! "$aws_region" =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]]; then
  echo "The AWS region is invalid." >&2
  exit 65
fi
if [[ ! "$app_image" =~ $immutable_image_pattern ]]; then
  echo "The application image must be the NitrosGames ECR image tagged with a full commit SHA." >&2
  exit 65
fi
if [[ ! -f "$compose_file" || ! -f "$configuration_file" ]]; then
  echo "Production compose.yaml and runtime.conf must exist in $deployment_directory." >&2
  exit 66
fi

read_config() {
  local key="$1"
  local value
  value="$(sed -n "s/^${key}=//p" "$configuration_file" | tail -n 1)"
  [[ -n "$value" ]] || {
    echo "Missing required non-secret configuration: $key" >&2
    return 1
  }
  printf '%s' "$value"
}

escape_env_value() {
  local value="$1"
  [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] || {
    echo "Runtime values cannot contain line breaks." >&2
    return 1
  }
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//\$/\$\$}"
  printf '"%s"' "$value"
}

write_env_line() {
  local key="$1"
  local value="$2"
  printf '%s=' "$key"
  escape_env_value "$value"
  printf '\n'
}

readonly secret_arn="$(read_config APPLICATION_DB_SECRET_ARN)"
readonly db_url="$(read_config DB_URL)"
readonly bucket="$(read_config APP_STORAGE_HOST_IMAGES_S3_BUCKET)"
readonly allowed_origins="$(read_config APP_SECURITY_ALLOWED_ORIGINS)"
readonly issuer_uri="$(read_config OAUTH2_ISSUER_URI)"
readonly jwk_set_uri="$(read_config OAUTH2_JWK_SET_URI)"
readonly resource_id="$(read_config OAUTH2_RESOURCE_ID)"
readonly access_scope="$(read_config OAUTH2_ACCESS_SCOPE)"
readonly admin_scope="$(read_config OAUTH2_ADMIN_SCOPE)"
readonly allowed_client_ids="$(read_config OAUTH2_ALLOWED_CLIENT_IDS)"

[[ "$db_url" == jdbc:mysql://*.rds.amazonaws.com:*\?*sslMode=VERIFY_IDENTITY* ]] || {
  echo "DB_URL must target the RDS DNS name and use sslMode=VERIFY_IDENTITY." >&2
  exit 67
}
[[ "$secret_arn" =~ ^arn:aws:secretsmanager:${aws_region}:[0-9]{12}:secret:nitros-games-backend/production/database/application-[A-Za-z0-9]{6}$ ]] || {
  echo "APPLICATION_DB_SECRET_ARN must identify the dedicated application credential in the selected region." >&2
  exit 67
}

registry="${app_image%%/*}"
if ! aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$registry"; then
  echo "DEPLOYMENT_RESULT=unchanged"
  exit 68
fi

secret_json="$(aws secretsmanager get-secret-value \
  --region "$aws_region" \
  --secret-id "$secret_arn" \
  --query SecretString \
  --output text)"
db_username="$(jq --exit-status --raw-output '.username' <<< "$secret_json")"
db_password="$(jq --exit-status --raw-output '.password' <<< "$secret_json")"
unset secret_json

[[ "$db_username" == "nitros_app" ]] || {
  unset db_password
  echo "The application database secret must contain username nitros_app." >&2
  exit 67
}

install -d -o root -g root -m 0700 "$runtime_directory"
candidate_environment="$(mktemp "$runtime_directory/runtime.env.XXXXXX")"
trap 'unset db_password; [[ -z "${candidate_environment:-}" || ! -f "$candidate_environment" ]] || rm -f "$candidate_environment"' EXIT
{
  write_env_line APP_IMAGE "$app_image"
  write_env_line AWS_REGION "$aws_region"
  write_env_line DB_URL "$db_url"
  write_env_line DB_USERNAME "$db_username"
  write_env_line DB_PASSWORD "$db_password"
  write_env_line APP_STORAGE_HOST_IMAGES_S3_BUCKET "$bucket"
  write_env_line APP_STORAGE_HOST_IMAGES_S3_PREFIX "host-images/"
  write_env_line APP_SECURITY_ALLOWED_ORIGINS "$allowed_origins"
  write_env_line OAUTH2_ISSUER_URI "$issuer_uri"
  write_env_line OAUTH2_JWK_SET_URI "$jwk_set_uri"
  write_env_line OAUTH2_RESOURCE_ID "$resource_id"
  write_env_line OAUTH2_ACCESS_SCOPE "$access_scope"
  write_env_line OAUTH2_ADMIN_SCOPE "$admin_scope"
  write_env_line OAUTH2_ALLOWED_CLIENT_IDS "$allowed_client_ids"
} > "$candidate_environment"
chmod 0600 "$candidate_environment"
unset db_password

compose() {
  local selected_environment="$1"
  shift
  docker compose --env-file "$selected_environment" --file "$compose_file" "$@"
}

if ! compose "$candidate_environment" config --quiet \
  || ! compose "$candidate_environment" pull api; then
  echo "DEPLOYMENT_RESULT=unchanged"
  echo "FAILED_IMAGE=$app_image"
  exit 69
fi

previous_image=""
if [[ -f "$active_image_file" ]]; then
  previous_image="$(<"$active_image_file")"
fi

mv "$candidate_environment" "$environment_file"
candidate_environment=""

start_and_verify() {
  compose "$environment_file" up --detach --no-deps --wait --wait-timeout 240 api \
    && curl --fail --silent --show-error \
      http://127.0.0.1:8080/actuator/health/readiness >/dev/null
}

capture_sanitized_candidate_logs() {
  compose "$environment_file" logs --no-color --tail 200 api 2>&1 \
    | sed -E \
      -e 's#(jdbc:mysql://)[^[:space:]]*#\1[REDACTED]#g' \
      -e '/password|secret|token|authorization/I s/.*/[REDACTED SENSITIVE LOG LINE]/' \
      > "$candidate_log_file" || true
  chmod 0600 "$candidate_log_file"
  echo "CANDIDATE_LOG=$candidate_log_file"
}

rm -f "$candidate_log_file"
if start_and_verify; then
  printf '%s\n' "$app_image" > "$active_image_file"
  chmod 0640 "$active_image_file"
  echo "DEPLOYMENT_RESULT=success"
  echo "DEPLOYED_IMAGE=$app_image"
  echo "PREVIOUS_IMAGE=${previous_image:-none}"
  exit 0
fi

echo "Candidate image failed readiness." >&2
capture_sanitized_candidate_logs
if [[ -n "$previous_image" && "$previous_image" =~ $immutable_image_pattern ]]; then
  temporary_environment="$(mktemp "$runtime_directory/runtime.rollback.XXXXXX")"
  sed "s|^APP_IMAGE=.*$|APP_IMAGE=\"$previous_image\"|" "$environment_file" > "$temporary_environment"
  chmod 0600 "$temporary_environment"
  mv "$temporary_environment" "$environment_file"
  if start_and_verify; then
    echo "DEPLOYMENT_RESULT=rolled_back"
    echo "FAILED_IMAGE=$app_image"
    echo "RESTORED_IMAGE=$previous_image"
    exit 70
  fi
  echo "DEPLOYMENT_RESULT=rollback_failed"
  exit 71
fi

echo "DEPLOYMENT_RESULT=rollback_unavailable"
echo "FAILED_IMAGE=$app_image"
exit 72
