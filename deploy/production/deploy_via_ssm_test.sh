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
  *"ssm send-command"*) printf 'command-123\n' ;;
  *"--query Status"*) printf 'Success\n' ;;
  *"ssm get-command-invocation"*)
    printf '%s\n' '{"StandardOutputContent":"DEPLOYMENT_RESULT=success\nDEPLOYED_IMAGE=123456789012.dkr.ecr.eu-west-1.amazonaws.com/nitros-games-backend:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\nFLYWAY_VERSION_OBSERVED=20\nFLYWAY_PHASE=after\n"}'
    ;;
  *) exit 1 ;;
esac
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
chmod +x "$mock_bin/aws" "$mock_bin/install"
export PATH="$mock_bin:$PATH"

readonly image="123456789012.dkr.ecr.eu-west-1.amazonaws.com/nitros-games-backend:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
output="$(bash "$script_directory/deploy-via-ssm.sh" i-0123456789abcdef0 "$image" eu-west-1 verify-restored-db)"
grep -q '^SSM_STATUS=Success$' <<<"$output"
grep -q '^DEPLOYMENT_RESULT=success$' <<<"$output"
grep -q '^FLYWAY_VERSION_OBSERVED=20$' <<<"$output"

if bash "$script_directory/deploy-via-ssm.sh" i-invalid "$image" eu-west-1 >/dev/null 2>&1; then
  echo "An invalid EC2 identifier was accepted." >&2
  exit 1
fi

echo "SSM deployment transport tests passed."
