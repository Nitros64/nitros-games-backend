#!/usr/bin/env bash
set -euo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly bootstrap_script="$script_directory/bootstrap-db-user.sh"
readonly test_root="$(mktemp -d)"
cleanup() {
  local status="$?"
  if [[ $status -ne 0 ]]; then
    for output_file in "$test_root"/*-output.log; do
      [[ -f "$output_file" ]] && cat "$output_file" >&2
    done
  fi
  rm -rf "$test_root"
  exit "$status"
}
trap cleanup EXIT

readonly account_id="123456789012"
readonly region="eu-west-1"
readonly master_secret_arn="arn:aws:secretsmanager:$region:$account_id:secret:rds!db-test"
readonly application_secret_arn="arn:aws:secretsmanager:$region:$account_id:secret:nitros-games-backend/production/database/application-AbCd12"
readonly fake_master_password="master-test-password"
readonly fake_application_password="A2345678901234567890123456789012345678901234567"
readonly deployment_directory="$test_root/deployment"
readonly runtime_directory="$test_root/runtime"
readonly mock_bin="$test_root/bin"

mkdir -p "$deployment_directory" "$runtime_directory" "$mock_bin"

cat >"$mock_bin/aws" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >>"$TEST_AWS_LOG"
case "$1 $2" in
  "secretsmanager get-secret-value")
    if [[ "$*" == *"$TEST_MASTER_SECRET_ARN"* ]]; then
      printf '{"username":"nitros_admin","password":"%s"}' "$TEST_MASTER_PASSWORD"
    elif [[ "${TEST_APP_SECRET_EXISTS:-false}" == "true" ]]; then
      printf '{"username":"nitros_app","password":"%s"}' "$TEST_APPLICATION_PASSWORD"
    else
      echo 'ResourceNotFoundException' >&2
      exit 254
    fi
    ;;
  "secretsmanager describe-secret") exit 0 ;;
  "secretsmanager get-random-password") printf '%s\n' "$TEST_APPLICATION_PASSWORD" ;;
  "secretsmanager put-secret-value")
    while [[ $# -gt 0 ]]; do
      if [[ "$1" == "--secret-string" ]]; then
        cp "${2#file://}" "$TEST_CAPTURED_SECRET"
        break
      fi
      shift
    done
    ;;
  *) exit 1 ;;
esac
EOF

cat >"$mock_bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
while [[ $# -gt 0 ]]; do
  if [[ "$1" == "--output" ]]; then
    printf '%s\n' 'test-ca-bundle' >"$2"
    exit 0
  fi
  shift
done
exit 1
EOF

cat >"$mock_bin/sha256sum" <<'EOF'
#!/usr/bin/env bash
cat >/dev/null
exit 0
EOF

cat >"$mock_bin/install" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
mkdir -p "${!#}"
EOF

cat >"$mock_bin/jq" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ "$*" == *"--rawfile password"* ]]; then
  password_file="${@: -2:1}"
  password="$(<"$password_file")"
  printf '{"username":"nitros_app","password":"%s"}\n' "$password"
elif [[ "$*" == *".username"* ]]; then
  source_file="${@: -1}"
  if grep -q 'nitros_admin' "$source_file"; then printf 'nitros_admin\n'; else printf 'nitros_app\n'; fi
elif [[ "$*" == *".password"* ]]; then
  source_file="${@: -1}"
  if grep -q 'nitros_admin' "$source_file"; then printf '%s\n' "$TEST_MASTER_PASSWORD"; else printf '%s\n' "$TEST_APPLICATION_PASSWORD"; fi
else
  exit 1
fi
EOF

cat >"$mock_bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >>"$TEST_DOCKER_LOG"
if [[ "$1" == "pull" ]]; then
  exit 0
fi
mount_value=""
previous=""
for argument in "$@"; do
  if [[ "$previous" == "--volume" ]]; then mount_value="$argument"; fi
  previous="$argument"
done
bootstrap_directory="${mount_value%:/bootstrap:ro}"
if [[ "$*" == *"master.cnf"* ]]; then
  cp "$bootstrap_directory/reconcile.sql" "$TEST_CAPTURED_SQL"
  [[ "${TEST_MYSQL_BOOTSTRAP_FAIL:-false}" != "true" ]]
else
  printf '1\nnitrosgames\nSsl_cipher\tTLS_AES_256_GCM_SHA384\n'
fi
EOF

chmod +x "$mock_bin/aws" "$mock_bin/curl" "$mock_bin/docker" "$mock_bin/install" "$mock_bin/jq" "$mock_bin/sha256sum"
export PATH="$mock_bin:$PATH"
export TEST_AWS_LOG="$test_root/aws.log"
export TEST_DOCKER_LOG="$test_root/docker.log"
export TEST_CAPTURED_SECRET="$test_root/captured-secret.json"
export TEST_CAPTURED_SQL="$test_root/reconcile.sql"
export TEST_MASTER_SECRET_ARN="$master_secret_arn"
export TEST_MASTER_PASSWORD="$fake_master_password"
export TEST_APPLICATION_PASSWORD="$fake_application_password"
export NITROS_GAMES_DEPLOYMENT_DIRECTORY="$deployment_directory"
export NITROS_GAMES_RUNTIME_DIRECTORY="$runtime_directory"

write_configuration() {
  cat >"$deployment_directory/runtime.conf" <<EOF
RDS_MASTER_SECRET_ARN=$master_secret_arn
APPLICATION_DB_SECRET_ARN=$application_secret_arn
DB_URL=jdbc:mysql://database.example.eu-west-1.rds.amazonaws.com:3306/nitrosgames?sslMode=VERIFY_IDENTITY
EOF
}

reset_case() {
  rm -f "$TEST_AWS_LOG" "$TEST_DOCKER_LOG" "$TEST_CAPTURED_SECRET" "$TEST_CAPTURED_SQL"
  unset TEST_APP_SECRET_EXISTS TEST_MYSQL_BOOTSTRAP_FAIL
  write_configuration
}

reset_case
initialization_output="$test_root/initialization-output.log"
bash "$bootstrap_script" "$region" >"$initialization_output" 2>&1
grep -q '^BOOTSTRAP_RESULT=success$' "$initialization_output"
grep -q 'secretsmanager get-random-password' "$TEST_AWS_LOG"
grep -q "secretsmanager put-secret-value --region $region --secret-id $application_secret_arn --secret-string file://" "$TEST_AWS_LOG"
grep -q '"username":"nitros_app"' "$TEST_CAPTURED_SECRET"
grep -q "GRANT ALL PRIVILEGES ON \`nitrosgames\`.\* TO 'nitros_app'@'%';" "$TEST_CAPTURED_SQL"
grep -q "REQUIRE SSL;" "$TEST_CAPTURED_SQL"

reset_case
export TEST_APP_SECRET_EXISTS=true
existing_output="$test_root/existing-output.log"
bash "$bootstrap_script" "$region" >"$existing_output" 2>&1
if grep -q 'get-random-password\|put-secret-value' "$TEST_AWS_LOG"; then
  echo "Existing application credentials were unexpectedly replaced." >&2
  exit 1
fi

reset_case
export TEST_MYSQL_BOOTSTRAP_FAIL=true
failure_output="$test_root/failure-output.log"
if bash "$bootstrap_script" "$region" >"$failure_output" 2>&1; then
  echo "MySQL reconciliation failure was ignored." >&2
  exit 1
fi
grep -q 'database reconciliation failed' "$failure_output"

rm -f "$deployment_directory/runtime.conf"
missing_configuration_output="$test_root/missing-configuration-output.log"
if bash "$bootstrap_script" "$region" >"$missing_configuration_output" 2>&1; then
  echo "Missing runtime configuration was accepted." >&2
  exit 1
fi

if grep -R -q "$fake_master_password\|$fake_application_password" \
  "$initialization_output" "$existing_output" "$failure_output" "$missing_configuration_output"; then
  echo "A database password was printed by the bootstrap script." >&2
  exit 1
fi
if grep -q "$fake_master_password\|$fake_application_password" "$TEST_AWS_LOG" "$TEST_DOCKER_LOG"; then
  echo "A database password was exposed in command arguments." >&2
  exit 1
fi

grep -q "get-secret-value --region $region --secret-id $master_secret_arn" "$TEST_AWS_LOG"
grep -q "describe-secret --region $region --secret-id $application_secret_arn" "$TEST_AWS_LOG"
grep -q 'mysql:8.4.11@sha256:3466ba4a4828aa8d46fb7c3bc16b67b781c98413cf4ea0fac6feaa6e881faa26' "$TEST_DOCKER_LOG"

echo "Production database-user bootstrap tests passed."
