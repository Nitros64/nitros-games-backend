#!/usr/bin/env bash
set -euo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly workflow="$script_directory/../../.github/workflows/cd-production.yml"

# shellcheck source=release_guard.sh
source "$script_directory/release_guard.sh"

expect_failure() {
  local label="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "$label unexpectedly succeeded." >&2
    exit 1
  fi
}

readonly valid_sha="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
readonly valid_digest="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

production_cd_require_main_ref "refs/heads/main"
production_cd_require_full_sha "$valid_sha"
production_cd_require_main_membership "ahead"
production_cd_require_main_membership "identical"
production_cd_require_successful_ci "success"
production_cd_require_ecr_image "$valid_digest"
production_cd_require_ssm_success "Success"
production_cd_require_deployment_success "success"
production_cd_require_http_200 "readiness" "200"

expect_failure "non-main workflow ref" production_cd_require_main_ref "refs/heads/feature"
expect_failure "short SHA" production_cd_require_full_sha "aaaaaaaa"
expect_failure "branch-only commit" production_cd_require_main_membership "diverged"
expect_failure "missing CI run" production_cd_require_successful_ci ""
expect_failure "failed CI run" production_cd_require_successful_ci "failure"
expect_failure "missing ECR image" production_cd_require_ecr_image "None"
expect_failure "failed SSM command" production_cd_require_ssm_success "Failed"
expect_failure "rolled-back deployment" production_cd_require_deployment_success "rolled_back"
expect_failure "failed public readiness" production_cd_require_http_200 "readiness" "503"

grep -q 'PRODUCTION_INSTANCE_ID' "$workflow"
if grep -q 'STAGING_INSTANCE_ID\|i-0b74bc66161f2ac55' "$workflow"; then
  echo "The production workflow references a staging deployment target." >&2
  exit 1
fi

for required_guard in \
  production_cd_require_main_ref \
  production_cd_require_full_sha \
  production_cd_require_main_membership \
  production_cd_require_successful_ci \
  production_cd_require_ecr_image \
  production_cd_require_ssm_success \
  production_cd_require_deployment_success \
  production_cd_require_http_200; do
  grep -q "$required_guard" "$workflow" || {
    echo "The production workflow does not invoke $required_guard." >&2
    exit 1
  }
done

if grep -q 'docker/build-push-action\|ecr get-login-password\|docker push' "$workflow"; then
  echo "Production CD must deploy an existing image rather than build or publish one." >&2
  exit 1
fi

echo "Production release guard and workflow target tests passed."
