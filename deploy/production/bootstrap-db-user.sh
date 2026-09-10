#!/usr/bin/env bash
set -euo pipefail

# Operational bootstrap tool only. It cannot run with the final application
# runtime role: a controlled elevation must temporarily restore exact master
# secret read, application secret write and GetRandomPassword permissions.

if [[ $# -ne 1 ]]; then
  echo "Usage: bootstrap-db-user.sh <aws-region>" >&2
  exit 64
fi

readonly aws_region="$1"
readonly application_username="nitros_app"
readonly deployment_directory="${NITROS_GAMES_DEPLOYMENT_DIRECTORY:-/opt/nitros-games}"
readonly runtime_directory="${NITROS_GAMES_RUNTIME_DIRECTORY:-/run/nitros-games}"
readonly configuration_file="$deployment_directory/runtime.conf"
readonly mysql_client_image="mysql:8.4.11@sha256:3466ba4a4828aa8d46fb7c3bc16b67b781c98413cf4ea0fac6feaa6e881faa26"
readonly rds_ca_bundle_url="https://truststore.pki.rds.amazonaws.com/eu-west-1/eu-west-1-bundle.pem"
readonly rds_ca_bundle_sha256="a11cf9a1d0aadd7db86f92cbaa496466daeb501bf1c5e429d8ce8914a01c15d6"
readonly excluded_password_characters=$'\'\\'

if [[ ! "$aws_region" =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]]; then
  echo "The AWS region is invalid." >&2
  exit 65
fi
if [[ "$aws_region" != "eu-west-1" ]]; then
  echo "The production database bootstrap is restricted to eu-west-1." >&2
  exit 65
fi
if [[ ! -f "$configuration_file" ]]; then
  echo "Production runtime.conf must exist in $deployment_directory." >&2
  exit 66
fi

for required_command in aws curl docker jq sha256sum; do
  command -v "$required_command" >/dev/null 2>&1 || {
    echo "Missing required command: $required_command" >&2
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

readonly master_secret_arn="$(read_config RDS_MASTER_SECRET_ARN)"
readonly application_secret_arn="$(read_config APPLICATION_DB_SECRET_ARN)"
readonly db_url="$(read_config DB_URL)"

[[ "$master_secret_arn" == arn:aws:secretsmanager:${aws_region}:*:secret:rds\!* ]] || {
  echo "RDS_MASTER_SECRET_ARN must identify the RDS-managed secret in the selected region." >&2
  exit 67
}
[[ "$application_secret_arn" =~ ^arn:aws:secretsmanager:${aws_region}:[0-9]{12}:secret:nitros-games-backend/production/database/application-[A-Za-z0-9]{6}$ ]] || {
  echo "APPLICATION_DB_SECRET_ARN must identify the dedicated application credential." >&2
  exit 67
}
[[ "$db_url" =~ ^jdbc:mysql://([a-zA-Z0-9.-]+\.rds\.amazonaws\.com):([0-9]+)/([^?]+)\?(.+)$ ]] || {
  echo "DB_URL must use the production RDS DNS name." >&2
  exit 67
}

readonly db_host="${BASH_REMATCH[1]}"
readonly db_port="${BASH_REMATCH[2]}"
readonly db_name="${BASH_REMATCH[3]}"
readonly db_query="${BASH_REMATCH[4]}"

[[ "$db_port" == "3306" && "$db_name" == "nitrosgames" ]] || {
  echo "DB_URL must target nitrosgames on TCP 3306." >&2
  exit 67
}
[[ "&$db_query&" == *"&sslMode=VERIFY_IDENTITY&"* ]] || {
  echo "DB_URL must require sslMode=VERIFY_IDENTITY." >&2
  exit 67
}

umask 077
install -d -o root -g root -m 0700 "$runtime_directory"
bootstrap_directory="$(mktemp -d "$runtime_directory/db-bootstrap.XXXXXX")"
master_secret_file="$bootstrap_directory/master-secret.json"
application_secret_file="$bootstrap_directory/application-secret.json"
generated_password_file="$bootstrap_directory/generated-password"
aws_error_file="$bootstrap_directory/aws-error.log"
mysql_error_file="$bootstrap_directory/mysql-error.log"
verification_file="$bootstrap_directory/verification.out"
ca_bundle_file="$bootstrap_directory/rds-ca-bundle.pem"

cleanup() {
  local status="$?"
  unset master_password application_password
  if [[ -n "${bootstrap_directory:-}" && "$bootstrap_directory" == "$runtime_directory"/db-bootstrap.* ]]; then
    rm -rf -- "$bootstrap_directory"
  fi
  exit "$status"
}
trap cleanup EXIT

curl --fail --location --retry 5 --silent --show-error \
  "$rds_ca_bundle_url" \
  --output "$ca_bundle_file"
printf '%s  %s\n' "$rds_ca_bundle_sha256" "$ca_bundle_file" \
  | sha256sum --check --strict >/dev/null

if ! aws secretsmanager get-secret-value \
  --region "$aws_region" \
  --secret-id "$master_secret_arn" \
  --query SecretString \
  --output text >"$master_secret_file" 2>"$aws_error_file"; then
  echo "Unable to retrieve the RDS bootstrap credential." >&2
  exit 68
fi

master_username="$(jq --exit-status --raw-output '.username' "$master_secret_file")"
master_password="$(jq --exit-status --raw-output '.password' "$master_secret_file")"
[[ -n "$master_username" && -n "$master_password" ]] || {
  echo "The RDS-managed secret has an invalid credential shape." >&2
  exit 68
}

if ! aws secretsmanager describe-secret \
  --region "$aws_region" \
  --secret-id "$application_secret_arn" >/dev/null 2>"$aws_error_file"; then
  echo "Unable to describe the application database secret." >&2
  exit 68
fi

if aws secretsmanager get-secret-value \
  --region "$aws_region" \
  --secret-id "$application_secret_arn" \
  --query SecretString \
  --output text >"$application_secret_file" 2>"$aws_error_file"; then
  application_secret_initialized=true
elif grep -q 'ResourceNotFoundException' "$aws_error_file"; then
  application_secret_initialized=false
else
  echo "Unable to inspect the application database credential." >&2
  exit 68
fi

if [[ "$application_secret_initialized" == "false" ]]; then
  if ! aws secretsmanager get-random-password \
    --region "$aws_region" \
    --password-length 48 \
    --exclude-characters "$excluded_password_characters" \
    --require-each-included-type \
    --query RandomPassword \
    --output text >"$generated_password_file" 2>"$aws_error_file"; then
    echo "Unable to generate the application database password." >&2
    exit 68
  fi
  jq --null-input \
    --arg username "$application_username" \
    --rawfile password "$generated_password_file" \
    '{username: $username, password: ($password | rtrimstr("\n"))}' \
    >"$application_secret_file"
  if ! aws secretsmanager put-secret-value \
    --region "$aws_region" \
    --secret-id "$application_secret_arn" \
    --secret-string "file://$application_secret_file" >/dev/null 2>"$aws_error_file"; then
    echo "Unable to initialize the application database credential." >&2
    exit 68
  fi
fi

stored_application_username="$(jq --exit-status --raw-output '.username' "$application_secret_file")"
application_password="$(jq --exit-status --raw-output '.password' "$application_secret_file")"
[[ "$stored_application_username" == "$application_username" ]] || {
  echo "The application secret username must be nitros_app." >&2
  exit 68
}
[[ ${#application_password} -ge 40 && ${#application_password} -le 64 ]] \
  && [[ "$application_password" != *"'"* ]] \
  && [[ "$application_password" != *'\'* ]] \
  && [[ "$application_password" != *$'\n'* && "$application_password" != *$'\r'* ]] || {
    echo "The application secret password does not satisfy the bootstrap safety policy." >&2
    exit 68
  }

escape_mysql_option_value() {
  local value="$1"
  [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] || return 1
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "$value"
}

write_mysql_config() {
  local destination="$1"
  local username="$2"
  local password="$3"
  {
    printf '[client]\n'
    printf 'user="%s"\n' "$(escape_mysql_option_value "$username")"
    printf 'password="%s"\n' "$(escape_mysql_option_value "$password")"
    printf 'host="%s"\n' "$db_host"
    printf 'port=%s\n' "$db_port"
    printf 'database="%s"\n' "$db_name"
    printf 'ssl-mode=VERIFY_IDENTITY\n'
    printf 'ssl-ca=/bootstrap/rds-ca-bundle.pem\n'
  } >"$destination"
  chmod 0600 "$destination"
}

write_mysql_config "$bootstrap_directory/master.cnf" "$master_username" "$master_password"
write_mysql_config "$bootstrap_directory/application.cnf" "$application_username" "$application_password"

{
  printf "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s' REQUIRE SSL;\n" \
    "$application_username" "$application_password"
  printf "ALTER USER '%s'@'%%' IDENTIFIED BY '%s' REQUIRE SSL;\n" \
    "$application_username" "$application_password"
  printf "GRANT ALL PRIVILEGES ON \`nitrosgames\`.* TO '%s'@'%%';\n" \
    "$application_username"
} >"$bootstrap_directory/reconcile.sql"
chmod 0600 "$bootstrap_directory/reconcile.sql"

docker pull "$mysql_client_image" >/dev/null

if ! docker run --rm --network bridge \
  --volume "$bootstrap_directory:/bootstrap:ro" \
  "$mysql_client_image" \
  mysql --defaults-extra-file=/bootstrap/master.cnf \
  --execute='source /bootstrap/reconcile.sql' \
  >"$mysql_error_file" 2>&1; then
  echo "The nitros_app database reconciliation failed." >&2
  exit 70
fi

if ! docker run --rm --network bridge \
  --volume "$bootstrap_directory:/bootstrap:ro" \
  "$mysql_client_image" \
  mysql --defaults-extra-file=/bootstrap/application.cnf \
  --batch --skip-column-names \
  --execute='SELECT 1; SELECT DATABASE(); SHOW STATUS LIKE "Ssl_cipher";' \
  >"$verification_file" 2>"$mysql_error_file"; then
  echo "The nitros_app credential verification failed." >&2
  exit 71
fi

grep -qx '1' "$verification_file" \
  && grep -qx 'nitrosgames' "$verification_file" \
  && grep -Eq '^Ssl_cipher[[:space:]]+[^[:space:]]+' "$verification_file" || {
    echo "The nitros_app TLS verification returned an unexpected result." >&2
    exit 71
  }

echo "BOOTSTRAP_RESULT=success"
echo "APPLICATION_DB_USER=nitros_app"
echo "TLS_VERIFIED=true"
