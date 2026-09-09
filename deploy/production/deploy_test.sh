#!/usr/bin/env bash
set -euo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly deploy_script="$script_directory/deploy.sh"
readonly test_root="$(mktemp -d)"
cleanup() {
  local status="$?"
  if [[ $status -ne 0 && -f "$test_root/output.log" ]]; then
    cat "$test_root/output.log" >&2
  fi
  rm -rf "$test_root"
  exit "$status"
}
trap cleanup EXIT

readonly account_id="123456789012"
readonly region="eu-west-1"
readonly image="$account_id.dkr.ecr.$region.amazonaws.com/nitros-games-backend:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
readonly secret_arn="arn:aws:secretsmanager:$region:$account_id:secret:rds!db-test"
readonly deployment_directory="$test_root/deployment"
readonly runtime_directory="$test_root/runtime"
readonly mock_bin="$test_root/bin"

mkdir -p "$deployment_directory" "$runtime_directory" "$mock_bin"
cp "$script_directory/compose.yaml" "$deployment_directory/compose.yaml"

cat > "$deployment_directory/runtime.conf" <<EOF
RDS_MASTER_SECRET_ARN=$secret_arn
DB_URL=jdbc:mysql://database.example.eu-west-1.rds.amazonaws.com:3306/nitrosgames?sslMode=VERIFY_IDENTITY
APP_STORAGE_HOST_IMAGES_S3_BUCKET=nitros-games-prod-host-images-$account_id-$region
APP_SECURITY_ALLOWED_ORIGINS=https://app.example.test
OAUTH2_ISSUER_URI=https://identity.example.test/realms/nitros-games
OAUTH2_JWK_SET_URI=https://identity.example.test/realms/nitros-games/protocol/openid-connect/certs
OAUTH2_AUDIENCE=nitros-games-api
EOF

cat > "$mock_bin/aws" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$TEST_AWS_LOG"
if [[ "$1 $2" == "ecr get-login-password" ]]; then
  printf 'temporary-login-token'
elif [[ "$1 $2" == "secretsmanager get-secret-value" ]]; then
  printf '{"username":"nitros_admin","password":"test-$-password"}'
else
  exit 1
fi
EOF

cat > "$mock_bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ "$1" == "login" ]]; then
  cat >/dev/null
fi
printf '%s\n' "$*" >> "$TEST_DOCKER_LOG"
EOF

cat > "$mock_bin/curl" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

cat > "$mock_bin/install" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
target="${!#}"
mkdir -p "$target"
EOF

cat > "$mock_bin/jq" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
cat >/dev/null
case "${*: -1}" in
  .username) printf 'nitros_admin\n' ;;
  .password) printf 'test-$-password\n' ;;
  *) exit 1 ;;
esac
EOF

chmod +x "$mock_bin/aws" "$mock_bin/docker" "$mock_bin/curl" "$mock_bin/install" "$mock_bin/jq"
export PATH="$mock_bin:$PATH"
export TEST_AWS_LOG="$test_root/aws.log"
export TEST_DOCKER_LOG="$test_root/docker.log"
export NITROS_GAMES_DEPLOYMENT_DIRECTORY="$deployment_directory"
export NITROS_GAMES_RUNTIME_DIRECTORY="$runtime_directory"

output="$test_root/output.log"
bash "$deploy_script" "$image" "$region" > "$output" 2>&1

grep -q '^DEPLOYMENT_RESULT=success$' "$output"
grep -q "secretsmanager get-secret-value --region $region --secret-id $secret_arn" "$TEST_AWS_LOG"
grep -q '^compose --env-file .* config --quiet$' "$TEST_DOCKER_LOG"
grep -q '^compose --env-file .* pull api$' "$TEST_DOCKER_LOG"
grep -q '^compose --env-file .* up --detach --no-deps --wait --wait-timeout 240 api$' "$TEST_DOCKER_LOG"
grep -q '^DB_PASSWORD="test-\$\$-password"$' "$runtime_directory/runtime.env"
if grep -q 'test-\$-password\|temporary-login-token' "$output"; then
  echo "A runtime secret was printed by the deployment script." >&2
  exit 1
fi

echo "Production deployment preflight, secret handling and readiness orchestration tests passed."
