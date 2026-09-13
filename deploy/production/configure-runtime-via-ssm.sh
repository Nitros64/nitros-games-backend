#!/usr/bin/env bash
set -euo pipefail

# Installs only the non-secret production runtime.conf through SSM. Database
# credentials remain in Secrets Manager and are resolved by deploy.sh on EC2.

if [[ $# -ne 3 ]]; then
  echo "Usage: configure-runtime-via-ssm.sh <instance-id> <aws-region> <configuration-json>" >&2
  exit 64
fi

readonly instance_id="$1"
readonly aws_region="$2"
readonly configuration_json="$3"

[[ "$instance_id" =~ ^i-[0-9a-f]{17}$ ]] || exit 65
[[ "$aws_region" == "eu-west-1" ]] || exit 65
jq --exit-status '
  type == "object"
  and (keys | sort == [
    "APPLICATION_DB_SECRET_ARN",
    "APP_SECURITY_ALLOWED_ORIGINS",
    "APP_STORAGE_HOST_IMAGES_S3_BUCKET",
    "DB_URL",
    "OAUTH2_ACCESS_SCOPE",
    "OAUTH2_ADMIN_SCOPE",
    "OAUTH2_ALLOWED_CLIENT_IDS",
    "OAUTH2_ISSUER_URI",
    "OAUTH2_JWK_SET_URI",
    "OAUTH2_RESOURCE_ID"
  ])
  and ([.[] | type == "string" and length > 0] | all)
  and (.DB_URL | test("^jdbc:mysql://[A-Za-z0-9.-]+\\.rds\\.amazonaws\\.com:3306/nitrosgames\\?.*sslMode=VERIFY_IDENTITY"))
  and (.APPLICATION_DB_SECRET_ARN | test("^arn:aws:secretsmanager:eu-west-1:[0-9]{12}:secret:nitros-games-backend/production/database/application-[A-Za-z0-9]{6}$"))
' <<<"$configuration_json" >/dev/null || {
  echo "The non-secret runtime configuration is invalid." >&2
  exit 67
}

configuration="$(jq --raw-output 'to_entries[] | "\(.key)=\(.value)"' <<<"$configuration_json" | tr -d '\r')"
[[ "$configuration" != *$'\r'* ]] || exit 67
encoded_configuration="$(printf '%s\n' "$configuration" | base64 --wrap=0)"

parameters="$(jq --compact-output --null-input \
  --arg content "$encoded_configuration" \
  '{commands: [
    "set -euo pipefail",
    "install -d -o root -g root -m 0750 /opt/nitros-games",
    "candidate=$(mktemp /opt/nitros-games/runtime.conf.XXXXXX)",
    "trap '\''rm -f \"$candidate\"'\'' EXIT",
    ("printf %s " + ($content | @sh) + " | base64 --decode > \"$candidate\""),
    "! grep -q $'\''\\r'\'' \"$candidate\"",
    "test \"$(wc -l < \"$candidate\")\" -eq 10",
    "chmod 0640 \"$candidate\"",
    "chown root:root \"$candidate\"",
    "mv -f \"$candidate\" /opt/nitros-games/runtime.conf",
    "trap - EXIT",
    "echo RUNTIME_CONFIGURATION=success"
  ]}')"

command_id="$(aws ssm send-command \
  --instance-ids "$instance_id" \
  --document-name AWS-RunShellScript \
  --comment "Install non-secret NitrosGames production runtime configuration" \
  --timeout-seconds 120 \
  --parameters "$parameters" \
  --query Command.CommandId \
  --output text)"

status="Pending"
for _ in $(seq 1 60); do
  status="$(aws ssm get-command-invocation \
    --command-id "$command_id" --instance-id "$instance_id" \
    --query Status --output text 2>/dev/null || true)"
  case "$status" in Success|Cancelled|Failed|TimedOut) break ;; esac
  sleep 2
done

result="$(aws ssm get-command-invocation \
  --command-id "$command_id" --instance-id "$instance_id" --output json)"
stdout="$(jq --raw-output '.StandardOutputContent' <<<"$result" | tr -d '\r')"
echo "SSM_COMMAND_ID=$command_id"
echo "SSM_STATUS=$status"
grep -q '^RUNTIME_CONFIGURATION=success$' <<<"$stdout" || exit 1
[[ "$status" == "Success" ]] || exit 1
