#!/usr/bin/env bash
set -euo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly deploy_script="$script_directory/deploy.sh"
readonly test_root="$(mktemp -d)"
readonly fake_bin="$test_root/bin"
readonly previous_image="123456789012.dkr.ecr.eu-west-1.amazonaws.com/nitros-games-backend:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
readonly candidate_image="123456789012.dkr.ecr.eu-west-1.amazonaws.com/nitros-games-backend:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

cleanup() {
  rm -rf "$test_root"
}
trap cleanup EXIT

mkdir -p "$fake_bin"

cat > "$fake_bin/aws" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf 'temporary-password\n'
EOF

cat > "$fake_bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" == "login" ]]; then
  cat > /dev/null
  exit 0
fi

if [[ "${1:-}" != "compose" ]]; then
  exit 0
fi

compose_command=""
for argument in "$@"; do
  case "$argument" in
    config|pull|up|ps)
      compose_command="$argument"
      break
      ;;
  esac
done

if [[ "$compose_command" == "ps" ]]; then
  echo "api healthy"
fi
EOF

cat > "$fake_bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

count=0
if [[ -f "$FAKE_CURL_COUNT_FILE" ]]; then
  count="$(cat "$FAKE_CURL_COUNT_FILE")"
fi
count=$((count + 1))
printf '%s' "$count" > "$FAKE_CURL_COUNT_FILE"

if (( count <= FAKE_CURL_FAILURES )); then
  exit 22
fi
printf '{"status":"UP"}'
EOF

chmod +x "$fake_bin/aws" "$fake_bin/docker" "$fake_bin/curl"

prepare_deployment() {
  local deployment_directory="$1"
  mkdir -p "$deployment_directory"
  printf 'services: {}\n' > "$deployment_directory/compose.yaml"
  cat > "$deployment_directory/.env" <<EOF
DB_USERNAME=nitros
DB_PASSWORD=test-password
DB_ROOT_PASSWORD=test-root-password
APP_SECURITY_ALLOWED_ORIGINS=http://localhost:4200
OAUTH2_ISSUER_URI=https://identity.invalid/realms/nitros-games
OAUTH2_JWK_SET_URI=https://identity.invalid/realms/nitros-games/protocol/openid-connect/certs
OAUTH2_AUDIENCE=nitros-games-api
APP_IMAGE=$previous_image
EOF
}

success_directory="$test_root/success"
prepare_deployment "$success_directory"
success_output="$test_root/success.out"
PATH="$fake_bin:$PATH" \
NITROS_GAMES_DEPLOYMENT_DIRECTORY="$success_directory" \
FAKE_CURL_COUNT_FILE="$test_root/success-curl-count" \
FAKE_CURL_FAILURES=0 \
  bash "$deploy_script" "$candidate_image" eu-west-1 > "$success_output" 2>&1

grep -q '^DEPLOYMENT_RESULT=success$' "$success_output"
grep -q "^DEPLOYED_IMAGE=$candidate_image$" "$success_output"
grep -q "^APP_IMAGE=$candidate_image$" "$success_directory/.env"

rollback_directory="$test_root/rollback"
prepare_deployment "$rollback_directory"
rollback_output="$test_root/rollback.out"
set +e
PATH="$fake_bin:$PATH" \
NITROS_GAMES_DEPLOYMENT_DIRECTORY="$rollback_directory" \
FAKE_CURL_COUNT_FILE="$test_root/rollback-curl-count" \
FAKE_CURL_FAILURES=1 \
  bash "$deploy_script" "$candidate_image" eu-west-1 > "$rollback_output" 2>&1
rollback_exit_code=$?
set -e

if [[ $rollback_exit_code -ne 70 ]]; then
  cat "$rollback_output" >&2
  echo "Expected rollback exit code 70, received $rollback_exit_code." >&2
  exit 1
fi

grep -q '^DEPLOYMENT_RESULT=rolled_back$' "$rollback_output"
grep -q "^FAILED_IMAGE=$candidate_image$" "$rollback_output"
grep -q "^RESTORED_IMAGE=$previous_image$" "$rollback_output"
grep -q "^APP_IMAGE=$previous_image$" "$rollback_directory/.env"

echo "Staging deployment success and rollback tests passed."
