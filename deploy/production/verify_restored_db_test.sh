#!/usr/bin/env bash
set -euo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly test_directory="$(mktemp -d)"
readonly deployment_directory="$test_directory/deployment"
readonly runtime_directory="$test_directory/runtime"
readonly mock_bin="$test_directory/bin"
trap 'rm -rf "$test_directory"' EXIT

mkdir -p "$deployment_directory" "$runtime_directory" "$mock_bin"
cat >"$deployment_directory/runtime.conf" <<'EOF'
APPLICATION_DB_SECRET_ARN=arn:aws:secretsmanager:eu-west-1:123456789012:secret:nitros-games-backend/production/database/application-Ab12Cd
DB_URL=jdbc:mysql://database.abcdefghijk.eu-west-1.rds.amazonaws.com:3306/nitrosgames?sslMode=VERIFY_IDENTITY
EOF

cat >"$mock_bin/aws" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
[[ "$*" == *"secretsmanager get-secret-value"* ]]
printf '%s\n' '{"username":"nitros_app","password":"safeMockPassword123456789012345678901234567890"}'
EOF

cat >"$mock_bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
destination=""
while [[ $# -gt 0 ]]; do
  if [[ "$1" == "--output" ]]; then destination="$2"; shift 2; else shift; fi
done
printf '%s\n' 'mock-ca' >"$destination"
EOF

cat >"$mock_bin/sha256sum" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

cat >"$mock_bin/install" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ " $* " == *" -d "* ]]; then
  mkdir -p "${!#}"
else
  destination="${!#}"
  source="${@: -2:1}"
  mkdir -p "$(dirname "$destination")"
  cp "$source" "$destination"
fi
EOF

cat >"$mock_bin/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ "$1" == "pull" ]]; then exit 0; fi
printf '0\n6\n6\n6\nSsl_cipher\tTLS_AES_256_GCM_SHA384\n'
EOF
chmod +x "$mock_bin"/*

export PATH="$mock_bin:$PATH"
export NITROS_GAMES_DEPLOYMENT_DIRECTORY="$deployment_directory"
export NITROS_GAMES_RUNTIME_DIRECTORY="$runtime_directory"

before_output="$(bash "$script_directory/verify-restored-db.sh" before eu-west-1)"
grep -q '^DATABASE_CONNECTIVITY=success$' <<<"$before_output"
grep -q '^FLYWAY_VERSION_OBSERVED=6$' <<<"$before_output"
[[ -f "$runtime_directory/flyway-restore-baseline" ]]

after_output="$(bash "$script_directory/verify-restored-db.sh" after eu-west-1)"
grep -q '^FLYWAY_PHASE=after$' <<<"$after_output"
[[ ! -e "$runtime_directory/flyway-restore-baseline" ]]

if grep -Eq 'FLYWAY(_VERSION)?=4([^0-9]|$)|ENGINE_VERSION=8\.4\.10' \
  "$script_directory/verify-restored-db.sh" \
  "$script_directory/../../.github/workflows/restore-production.yml"; then
  echo "Restore validation must not pin Flyway V4 or MySQL 8.4.10." >&2
  exit 1
fi

echo "Dynamic restored database verification tests passed."
