#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: deploy.sh <immutable-ecr-image> <aws-region>" >&2
  exit 64
fi

readonly app_image="$1"
readonly aws_region="$2"
readonly deployment_directory="/opt/nitros-games"
readonly environment_file="$deployment_directory/.env"
readonly compose_file="$deployment_directory/compose.yaml"

if [[ ! "$app_image" =~ ^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com/[a-z0-9._/-]+:[0-9a-f]{40}$ ]]; then
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

temporary_environment="$(mktemp "$deployment_directory/.env.XXXXXX")"
grep -v '^APP_IMAGE=' "$environment_file" > "$temporary_environment" || true
printf 'APP_IMAGE=%s\n' "$app_image" >> "$temporary_environment"
chmod 0600 "$temporary_environment"
mv "$temporary_environment" "$environment_file"

registry="${app_image%%/*}"
aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$registry"

cd "$deployment_directory"
docker compose --env-file "$environment_file" --file "$compose_file" config --quiet
docker compose --env-file "$environment_file" --file "$compose_file" pull
docker compose --env-file "$environment_file" --file "$compose_file" up \
  --detach --remove-orphans --wait --wait-timeout 240

curl --fail --silent --show-error \
  http://127.0.0.1:8080/actuator/health/readiness
echo

docker image prune --force
docker compose --env-file "$environment_file" --file "$compose_file" ps
