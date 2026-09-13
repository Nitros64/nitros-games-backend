#!/usr/bin/env bash
set -euo pipefail

# Runs on the production EC2 instance before and after the first application
# start following an RDS restore. It validates the restored schema generically;
# no Flyway version is treated as a permanent release contract.

if [[ $# -ne 2 || ! "$1" =~ ^(before|after)$ ]]; then
  echo "Usage: verify-restored-db.sh <before|after> <aws-region>" >&2
  exit 64
fi

readonly phase="$1"
readonly aws_region="$2"
readonly deployment_directory="${NITROS_GAMES_DEPLOYMENT_DIRECTORY:-/opt/nitros-games}"
readonly runtime_directory="${NITROS_GAMES_RUNTIME_DIRECTORY:-/run/nitros-games}"
readonly configuration_file="$deployment_directory/runtime.conf"
readonly baseline_file="$runtime_directory/flyway-restore-baseline"
readonly mysql_client_image="mysql:8.4.11@sha256:3466ba4a4828aa8d46fb7c3bc16b67b781c98413cf4ea0fac6feaa6e881faa26"
readonly rds_ca_bundle_url="https://truststore.pki.rds.amazonaws.com/eu-west-1/eu-west-1-bundle.pem"
readonly rds_ca_bundle_sha256="a11cf9a1d0aadd7db86f92cbaa496466daeb501bf1c5e429d8ce8914a01c15d6"

[[ "$aws_region" == "eu-west-1" ]] || {
  echo "Restored database verification is restricted to production eu-west-1." >&2
  exit 65
}
[[ -f "$configuration_file" ]] || {
  echo "Production runtime.conf is missing." >&2
  exit 66
}

for required in aws curl docker jq sha256sum; do
  command -v "$required" >/dev/null 2>&1 || {
    echo "Missing required command: $required" >&2
    exit 69
  }
done

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

readonly secret_arn="$(read_config APPLICATION_DB_SECRET_ARN)"
readonly db_url="$(read_config DB_URL)"

[[ "$secret_arn" =~ ^arn:aws:secretsmanager:${aws_region}:[0-9]{12}:secret:nitros-games-backend/production/database/application-[A-Za-z0-9]{6}$ ]] || {
  echo "APPLICATION_DB_SECRET_ARN is invalid." >&2
  exit 67
}
[[ "$db_url" =~ ^jdbc:mysql://([a-zA-Z0-9.-]+\.rds\.amazonaws\.com):3306/nitrosgames\?(.+)$ ]] || {
  echo "DB_URL must target the production RDS hostname and nitrosgames database." >&2
  exit 67
}
readonly db_host="${BASH_REMATCH[1]}"
readonly db_query="${BASH_REMATCH[2]}"
[[ "&$db_query&" == *"&sslMode=VERIFY_IDENTITY&"* ]] || {
  echo "DB_URL must require sslMode=VERIFY_IDENTITY." >&2
  exit 67
}

umask 077
install -d -o root -g root -m 0700 "$runtime_directory"
verification_directory="$(mktemp -d "$runtime_directory/db-restore-check.XXXXXX")"
credential_file="$verification_directory/application-secret.json"
mysql_config="$verification_directory/application.cnf"
ca_bundle="$verification_directory/rds-ca-bundle.pem"
query_result="$verification_directory/query.tsv"

cleanup() {
  local status="$?"
  unset db_password
  if [[ -n "${verification_directory:-}" && "$verification_directory" == "$runtime_directory"/db-restore-check.* ]]; then
    rm -rf -- "$verification_directory"
  fi
  exit "$status"
}
trap cleanup EXIT

aws secretsmanager get-secret-value \
  --region "$aws_region" \
  --secret-id "$secret_arn" \
  --query SecretString \
  --output text >"$credential_file"
db_username="$(jq --exit-status --raw-output '.username' "$credential_file")"
db_password="$(jq --exit-status --raw-output '.password' "$credential_file")"
[[ "$db_username" == "nitros_app" && -n "$db_password" ]] || {
  echo "The application database credential has an invalid shape." >&2
  exit 68
}

escape_mysql_option_value() {
  local value="$1"
  [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] || return 1
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "$value"
}

{
  printf '[client]\n'
  printf 'user="%s"\n' "$(escape_mysql_option_value "$db_username")"
  printf 'password="%s"\n' "$(escape_mysql_option_value "$db_password")"
  printf 'host="%s"\n' "$db_host"
  printf 'port=3306\n'
  printf 'database="nitrosgames"\n'
  printf 'ssl-mode=VERIFY_IDENTITY\n'
  printf 'ssl-ca=/check/rds-ca-bundle.pem\n'
} >"$mysql_config"
unset db_password

curl --fail --location --retry 5 --silent --show-error \
  "$rds_ca_bundle_url" --output "$ca_bundle"
printf '%s  %s\n' "$rds_ca_bundle_sha256" "$ca_bundle" \
  | sha256sum --check --strict >/dev/null
docker pull "$mysql_client_image" >/dev/null

readonly flyway_query=$'SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;\nSELECT COUNT(*) FROM flyway_schema_history;\nSELECT COALESCE(MAX(installed_rank), 0) FROM flyway_schema_history;\nSELECT COALESCE((SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1), "NONE");\nSHOW STATUS LIKE "Ssl_cipher";'

if ! docker run --rm --network bridge \
  --volume "$verification_directory:/check:ro" \
  "$mysql_client_image" \
  mysql --defaults-extra-file=/check/application.cnf \
  --batch --skip-column-names --execute="$flyway_query" \
  >"$query_result"; then
  echo "The restored database could not be queried as nitros_app over verified TLS." >&2
  exit 70
fi

mapfile -t results <"$query_result"
[[ "${#results[@]}" -eq 5 ]] || {
  echo "The Flyway verification returned an unexpected result shape." >&2
  exit 71
}
readonly failed_count="${results[0]}"
readonly migration_count="${results[1]}"
readonly installed_rank="${results[2]}"
readonly observed_version="${results[3]}"
readonly ssl_status="${results[4]}"

[[ "$failed_count" == "0" ]] || {
  echo "flyway_schema_history contains failed migrations." >&2
  exit 72
}
[[ "$migration_count" =~ ^[0-9]+$ && "$migration_count" -gt 0 ]] || {
  echo "flyway_schema_history is missing or empty." >&2
  exit 72
}
[[ "$installed_rank" =~ ^[0-9]+$ ]] || exit 72
[[ "$ssl_status" =~ ^Ssl_cipher[[:space:]]+[^[:space:]]+ ]] || {
  echo "The database connection did not negotiate TLS." >&2
  exit 72
}

if [[ "$phase" == "before" ]]; then
  printf '%s\t%s\n' "$migration_count" "$installed_rank" >"$baseline_file"
  chmod 0600 "$baseline_file"
else
  [[ -f "$baseline_file" ]] || {
    echo "The pre-start Flyway baseline is missing." >&2
    exit 73
  }
  IFS=$'\t' read -r previous_count previous_rank <"$baseline_file"
  [[ "$migration_count" -ge "$previous_count" && "$installed_rank" -ge "$previous_rank" ]] || {
    echo "Flyway history regressed after application startup." >&2
    exit 73
  }
  rm -f "$baseline_file"
fi

echo "DATABASE_CONNECTIVITY=success"
echo "RDS_TLS=verified"
echo "FLYWAY_FAILED_MIGRATIONS=0"
echo "FLYWAY_MIGRATION_COUNT=$migration_count"
echo "FLYWAY_VERSION_OBSERVED=$observed_version"
echo "FLYWAY_PHASE=$phase"
