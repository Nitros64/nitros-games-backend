#!/usr/bin/env bash
set -euo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly repository_root="$script_directory/../.."
# shellcheck source=lifecycle.sh
source "$script_directory/lifecycle.sh"

expect_failure() {
  local label="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "$label unexpectedly succeeded." >&2
    exit 1
  fi
}

readonly valid_state='{"schemaVersion":1,"desiredState":"HIBERNATED","lastTransitionRun":null,"updatedAt":"2026-09-13T00:00:00Z"}'
readonly active_state='{"schemaVersion":1,"desiredState":"ACTIVE","lastTransitionRun":"42","updatedAt":"2026-09-13T01:00:00Z"}'
readonly valid_release='{"schemaVersion":1,"applicationSha":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","imageDigest":"sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","updatedAt":"2026-09-13T00:00:00Z"}'
readonly TEST_APPLICATION_SHA='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
readonly TEST_RESTORE_SNAPSHOT='nitros-games-backend-production-hibernation-run-100'

production_validate_lifecycle_json "$valid_state"
production_validate_lifecycle_json "$active_state"
production_validate_release_json "$valid_release"
production_require_desired_state HIBERNATED "$valid_state"
production_require_hibernate_confirmation HIBERNATE_PRODUCTION
production_require_full_sha aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

expect_failure "unknown desired state" production_validate_lifecycle_json \
  '{"schemaVersion":1,"desiredState":"STOPPED","lastTransitionRun":null,"updatedAt":"x"}'
expect_failure "extra lifecycle key" production_validate_lifecycle_json \
  '{"schemaVersion":1,"desiredState":"ACTIVE","lastTransitionRun":null,"updatedAt":"x","image":"x"}'
expect_failure "release metadata without digest" production_validate_release_json \
  '{"schemaVersion":1,"applicationSha":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","updatedAt":"x"}'
expect_failure "wrong destructive confirmation" production_require_hibernate_confirmation HIBERNATE
expect_failure "wrong desired state" production_require_desired_state ACTIVE "$valid_state"

restore_marker="$(production_restore_transition_marker 100 "$TEST_RESTORE_SNAPSHOT" "$TEST_APPLICATION_SHA")"
partial_restore_state="$(jq --compact-output --arg marker "$restore_marker" \
  '.desiredState = "ACTIVE" | .lastTransitionRun = $marker' <<< "$valid_state")"
[[ "$(production_require_resumable_restore "$partial_restore_state" "$TEST_RESTORE_SNAPSHOT" "$TEST_APPLICATION_SHA")" == "100" ]]
expect_failure "restore with changed snapshot" production_require_resumable_restore \
  "$partial_restore_state" nitros-games-backend-production-hibernation-run-101 "$TEST_APPLICATION_SHA"
expect_failure "restore with changed SHA" production_require_resumable_restore \
  "$partial_restore_state" "$TEST_RESTORE_SNAPSHOT" bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb

hibernate_marker="$(production_hibernate_transition_marker 200)"
partial_hibernate_state="$(jq --compact-output --arg marker "$hibernate_marker" \
  '.lastTransitionRun = $marker' <<< "$valid_state")"
[[ "$(production_resolve_hibernate_operation_run "$active_state" 200)" == "200" ]]
# A new workflow run (201) must resume the original operation (200) so that it
# reuses the same recovery/final snapshot names.
[[ "$(production_resolve_hibernate_operation_run "$partial_hibernate_state" 201)" == "200" ]]
expect_failure "hibernation without resumable marker" production_resolve_hibernate_operation_run "$valid_state" 201

[[ "$(production_resolve_start_action "$active_state" STOPPED)" == "START" ]]
[[ "$(production_resolve_start_action "$active_state" ACTIVE)" == "VERIFY" ]]
expect_failure "start while hibernated" production_resolve_start_action "$valid_state" HIBERNATED
expect_failure "start from inconsistent active state" production_resolve_start_action "$active_state" INCONSISTENT
[[ "$(production_resolve_stop_action "$active_state" ACTIVE)" == "STOP" ]]
[[ "$(production_resolve_stop_action "$active_state" STOPPED)" == "VERIFY_STOPPED" ]]
[[ "$(production_resolve_stop_action "$valid_state" HIBERNATED)" == "ALREADY_HIBERNATED" ]]
expect_failure "stop during incomplete hibernation" production_resolve_stop_action "$valid_state" INCONSISTENT

temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

make_plan() {
  local destination="$1"
  shift
  jq --null-input --argjson changes "$*" '{resource_changes: $changes}' >"$destination"
}

make_plan "$temporary_directory/stable.json" '[]'
production_require_plan_mode stable "$temporary_directory/stable.json"

make_plan "$temporary_directory/rds-create.json" '[{"address":"aws_db_instance.mysql[0]","change":{"actions":["create"]}}]'
production_require_plan_mode restore-data "$temporary_directory/rds-create.json"
production_require_plan_mode converge-restore-data "$temporary_directory/rds-create.json"

make_plan "$temporary_directory/rds-create-replacement.json" '[{"address":"aws_db_instance.mysql[0]","change":{"actions":["delete","create"]}}]'
expect_failure "RDS delete-create replacement" production_require_plan_mode restore-data "$temporary_directory/rds-create-replacement.json"
make_plan "$temporary_directory/rds-delete-replacement.json" '[{"address":"aws_db_instance.mysql[0]","change":{"actions":["create","delete"]}}]'
expect_failure "RDS create-delete replacement" production_require_plan_mode delete-data "$temporary_directory/rds-delete-replacement.json"

readonly final_snapshot='nitros-games-backend-production-hibernation-final-run-200'
make_plan "$temporary_directory/rds-protect.json" '[{
  "address":"aws_db_instance.mysql[0]",
  "change":{
    "actions":["update"],
    "before":{"deletion_protection":true,"final_snapshot_identifier":null,"instance_class":"db.t4g.micro"},
    "after":{"deletion_protection":false,"final_snapshot_identifier":"nitros-games-backend-production-hibernation-final-run-200","instance_class":"db.t4g.micro"}
  }
}]'
production_require_plan_mode protect-data "$temporary_directory/rds-protect.json" "$final_snapshot"

make_plan "$temporary_directory/rds-unrelated-update.json" '[{
  "address":"aws_db_instance.mysql[0]",
  "change":{
    "actions":["update"],
    "before":{"deletion_protection":true,"final_snapshot_identifier":null,"instance_class":"db.t4g.micro"},
    "after":{"deletion_protection":false,"final_snapshot_identifier":"nitros-games-backend-production-hibernation-final-run-200","instance_class":"db.t4g.small"}
  }
}]'
expect_failure "unrelated RDS update" production_require_plan_mode protect-data \
  "$temporary_directory/rds-unrelated-update.json" "$final_snapshot"

make_plan "$temporary_directory/runtime-create.json" '[
  {"address":"aws_instance.application[0]","change":{"actions":["create"]}},
  {"address":"aws_route53_record.api_certificate_validation[\"api.nitrosgames64.com\"]","change":{"actions":["create"]}}
]'
production_require_plan_mode converge-restore-runtime "$temporary_directory/runtime-create.json"

make_plan "$temporary_directory/runtime-update.json" '[{"address":"aws_instance.application[0]","change":{"actions":["update"]}}]'
expect_failure "runtime update during restore" production_require_plan_mode converge-restore-runtime "$temporary_directory/runtime-update.json"

runtime_addresses=(
  'aws_instance.application[0]'
  'aws_iam_role.application[0]'
  'aws_iam_role_policy_attachment.ssm_core[0]'
  'aws_iam_role_policy.ecr_pull[0]'
  'aws_iam_role_policy.host_images[0]'
  'aws_iam_role_policy.application_database_secret[0]'
  'aws_iam_instance_profile.application[0]'
  'aws_iam_role.github_production_deployer[0]'
  'aws_iam_role_policy.github_production_deploy[0]'
  'aws_security_group.load_balancer[0]'
  'aws_vpc_security_group_ingress_rule.load_balancer_https_ipv4[0]'
  'aws_vpc_security_group_ingress_rule.load_balancer_http_redirect_ipv4[0]'
  'aws_vpc_security_group_egress_rule.load_balancer_to_application[0]'
  'aws_vpc_security_group_ingress_rule.application_from_load_balancer[0]'
  'aws_lb.application[0]'
  'aws_lb_target_group.application[0]'
  'aws_lb_target_group_attachment.application[0]'
  'aws_acm_certificate.api[0]'
  'aws_acm_certificate_validation.api[0]'
  'aws_lb_listener.https[0]'
  'aws_lb_listener.http_redirect[0]'
  'aws_route53_record.api[0]'
  'aws_route53_record.api_certificate_validation["api.nitrosgames64.com"]'
)
runtime_create_changes='[]'
runtime_delete_changes='[]'
for address in "${runtime_addresses[@]}"; do
  runtime_create_changes="$(jq --compact-output --arg address "$address" \
    '. + [{address: $address, change: {actions: ["create"]}}]' <<< "$runtime_create_changes")"
  runtime_delete_changes="$(jq --compact-output --arg address "$address" \
    '. + [{address: $address, change: {actions: ["delete"]}}]' <<< "$runtime_delete_changes")"
done
make_plan "$temporary_directory/runtime-create-exact.json" "$runtime_create_changes"
make_plan "$temporary_directory/runtime-delete-exact.json" "$runtime_delete_changes"
production_require_plan_mode restore-runtime "$temporary_directory/runtime-create-exact.json"
production_require_plan_mode delete-runtime "$temporary_directory/runtime-delete-exact.json"

make_plan "$temporary_directory/unexpected.json" '[{"address":"aws_s3_bucket.host_images","change":{"actions":["delete"]}}]'
expect_failure "unexpected durable destroy" production_require_plan_mode converge-delete-runtime "$temporary_directory/unexpected.json"
expect_failure "unexpected data change" production_require_plan_mode converge-delete-data "$temporary_directory/unexpected.json"

for workflow in \
  "$repository_root/.github/workflows/cd-production.yml" \
  "$repository_root/.github/workflows/start-production.yml" \
  "$repository_root/.github/workflows/stop-production.yml" \
  "$repository_root/.github/workflows/hibernate-production.yml" \
  "$repository_root/.github/workflows/restore-production.yml"; do
  grep -q 'group: production-control' "$workflow"
  if grep -q 'PRODUCTION_INSTANCE_ID' "$workflow"; then
    echo "Static production instance ID found in $workflow." >&2
    exit 1
  fi
done

grep -q 'production_transition_desired_state ACTIVE HIBERNATED' \
  "$repository_root/.github/workflows/hibernate-production.yml"
grep -q 'production_transition_desired_state HIBERNATED ACTIVE' \
  "$repository_root/.github/workflows/restore-production.yml"
grep -q 'normalized-data' "$repository_root/.github/workflows/restore-production.yml"
grep -q 'production_require_resumable_restore' "$repository_root/.github/workflows/restore-production.yml"
grep -q 'production_resolve_hibernate_operation_run' "$repository_root/.github/workflows/hibernate-production.yml"
grep -q "needs_start == 'true'" "$repository_root/.github/workflows/start-production.yml"
grep -q "needs_stop == 'true'" "$repository_root/.github/workflows/stop-production.yml"
if grep -Eq 'production_transition_desired_state' \
  "$repository_root/.github/workflows/start-production.yml" \
  "$repository_root/.github/workflows/stop-production.yml"; then
  echo "Start/Stop must not mutate persistent desiredState." >&2
  exit 1
fi

for variables in \
  "$repository_root/infra/terraform/production/data/variables.tf" \
  "$repository_root/infra/terraform/production/runtime/variables.tf"; do
  if grep -Eq 'variable "(database|runtime)_enabled"' "$variables"; then
    echo "An independent enabled variable remains in $variables." >&2
    exit 1
  fi
done

readonly foundation_control="$repository_root/infra/terraform/production/foundation/production-lifecycle-control.tf"
readonly runtime_iam="$repository_root/infra/terraform/production/runtime/iam.tf"
grep -q '/${var.project_name}/${var.environment}/lifecycle/state' "$foundation_control"
grep -q '/${var.project_name}/${var.environment}/lifecycle/release' "$foundation_control"
grep -q 'production_release_parameter_arn' "$runtime_iam"
if grep -q 'resource "aws_iam_role_policy" "github_production_lifecycle_' "$foundation_control"; then
  echo "Lifecycle permissions must not consume the aggregate inline-policy quota." >&2
  exit 1
fi
[[ "$(grep -c 'resource "aws_iam_policy" "github_production_lifecycle_' "$foundation_control")" -eq 4 ]]
[[ "$(grep -c 'production_lifecycle_state_parameter_arn' "$runtime_iam")" -eq 1 ]]
[[ "$(grep -c 'production_release_parameter_arn' "$runtime_iam")" -eq 2 ]]

for tag in 'Name,Values=nitros-games-backend-production-application' \
  'Project,Values=$PRODUCTION_PROJECT' 'Environment,Values=$PRODUCTION_ENVIRONMENT' \
  'Component,Values=production-runtime'; do
  grep -Fq "$tag" "$script_directory/lifecycle.sh"
done

echo "Production lifecycle guards and workflow contracts passed."
