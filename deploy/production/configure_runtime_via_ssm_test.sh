#!/usr/bin/env bash
set -euo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly test_directory="$(mktemp -d)"
readonly mock_bin="$test_directory/bin"
trap 'rm -rf "$test_directory"' EXIT
mkdir -p "$mock_bin"

cat >"$mock_bin/aws" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
case "$*" in
  *"ssm send-command"*) printf 'command-configuration\n' ;;
  *"--query Status"*) printf 'Success\n' ;;
  *"ssm get-command-invocation"*)
    printf '%s\n' '{"StandardOutputContent":"RUNTIME_CONFIGURATION=success\n"}'
    ;;
  *) exit 1 ;;
esac
EOF
chmod +x "$mock_bin/aws"
export PATH="$mock_bin:$PATH"

readonly configuration='{
  "APPLICATION_DB_SECRET_ARN":"arn:aws:secretsmanager:eu-west-1:123456789012:secret:nitros-games-backend/production/database/application-Ab12Cd",
  "APP_SECURITY_ALLOWED_ORIGINS":"https://nitrosgames64.com",
  "APP_STORAGE_HOST_IMAGES_S3_BUCKET":"nitros-games-prod-host-images-123456789012-eu-west-1",
  "DB_URL":"jdbc:mysql://database.example.eu-west-1.rds.amazonaws.com:3306/nitrosgames?sslMode=VERIFY_IDENTITY",
  "OAUTH2_ACCESS_SCOPE":"https://api.nitrosgames64.com/access",
  "OAUTH2_ADMIN_SCOPE":"https://api.nitrosgames64.com/admin",
  "OAUTH2_ALLOWED_CLIENT_IDS":"client-id",
  "OAUTH2_ISSUER_URI":"https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_example",
  "OAUTH2_JWK_SET_URI":"https://cognito-idp.eu-west-1.amazonaws.com/eu-west-1_example/.well-known/jwks.json",
  "OAUTH2_RESOURCE_ID":"https://api.nitrosgames64.com"
}'

if ! output="$(bash "$script_directory/configure-runtime-via-ssm.sh" \
  i-0123456789abcdef0 eu-west-1 "$configuration" 2>"$test_directory/configuration-error.log")"; then
  cat "$test_directory/configuration-error.log" >&2
  exit 1
fi
grep -q '^SSM_STATUS=Success$' <<<"$output"

if bash "$script_directory/configure-runtime-via-ssm.sh" \
  i-0123456789abcdef0 eu-west-1 '{}' >/dev/null 2>&1; then
  echo "An incomplete runtime configuration was accepted." >&2
  exit 1
fi

echo "Non-secret SSM runtime configuration tests passed."
