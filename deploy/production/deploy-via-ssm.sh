#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 || $# -gt 4 ]]; then
  echo "Usage: deploy-via-ssm.sh <instance-id> <immutable-ecr-image> <aws-region> [verify-restored-db]" >&2
  exit 64
fi

readonly instance_id="$1"
readonly app_image="$2"
readonly aws_region="$3"
readonly deployment_mode="${4:-deploy}"
readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

[[ "$instance_id" =~ ^i-[0-9a-f]{17}$ ]] || {
  echo "A valid dynamically discovered EC2 instance ID is required." >&2
  exit 65
}
[[ "$aws_region" =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]] || exit 65
[[ "$app_image" =~ ^[0-9]{12}\.dkr\.ecr\.${aws_region}\.amazonaws\.com/nitros-games-backend:[0-9a-f]{40}$ ]] || {
  echo "The deployment image must use an immutable full-SHA ECR tag." >&2
  exit 65
}
[[ "$deployment_mode" == "deploy" || "$deployment_mode" == "verify-restored-db" ]] || exit 65

for required in aws base64 jq tar; do
  command -v "$required" >/dev/null 2>&1 || {
    echo "Missing required command: $required" >&2
    exit 69
  }
done

work_directory="$(mktemp -d)"
cleanup() {
  rm -rf -- "$work_directory"
}
trap cleanup EXIT

install -d -m 0750 "$work_directory/bundle"
install -m 0644 "$script_directory/compose.yaml" "$work_directory/bundle/compose.yaml"
install -m 0750 "$script_directory/deploy.sh" "$work_directory/bundle/deploy.sh"
install -m 0750 "$script_directory/verify-restored-db.sh" "$work_directory/bundle/verify-restored-db.sh"
tar --create --gzip --file "$work_directory/production-deploy.tgz" --directory "$work_directory/bundle" .
bundle="$(base64 --wrap=0 "$work_directory/production-deploy.tgz")"
archive="/tmp/nitros-games-production-${GITHUB_RUN_ID:-manual}.tgz"
bundle_dir="/tmp/nitros-games-production-${GITHUB_RUN_ID:-manual}"

parameters="$(jq --compact-output --null-input \
  --arg bundle "$bundle" \
  --arg image "$app_image" \
  --arg region "$aws_region" \
  --arg archive "$archive" \
  --arg bundle_dir "$bundle_dir" \
  --arg mode "$deployment_mode" \
  '{commands: [
    "set -euo pipefail",
    ("archive=" + ($archive | @sh)),
    ("bundle_dir=" + ($bundle_dir | @sh)),
    "cleanup() { rm -rf \"$bundle_dir\" \"$archive\" /opt/nitros-games/compose.yaml.candidate /opt/nitros-games/deploy.sh.candidate /opt/nitros-games/verify-restored-db.sh.candidate; }",
    "trap cleanup EXIT",
    "install -d -m 0750 /opt/nitros-games",
    ("printf %s " + ($bundle | @sh) + " | base64 --decode > \"$archive\""),
    "rm -rf \"$bundle_dir\"",
    "install -d -m 0700 \"$bundle_dir\"",
    "tar --extract --gzip --file \"$archive\" --directory \"$bundle_dir\"",
    "bash -n \"$bundle_dir/deploy.sh\"",
    "bash -n \"$bundle_dir/verify-restored-db.sh\"",
    "grep -qx '\''      - 8080:8080'\'' \"$bundle_dir/compose.yaml\"",
    "! grep -q '\''127\\.0\\.0\\.1:8080:8080'\'' \"$bundle_dir/compose.yaml\"",
    "install -o root -g root -m 0640 \"$bundle_dir/compose.yaml\" /opt/nitros-games/compose.yaml.candidate",
    "install -o root -g root -m 0750 \"$bundle_dir/deploy.sh\" /opt/nitros-games/deploy.sh.candidate",
    "install -o root -g root -m 0750 \"$bundle_dir/verify-restored-db.sh\" /opt/nitros-games/verify-restored-db.sh.candidate",
    "mv -f /opt/nitros-games/compose.yaml.candidate /opt/nitros-games/compose.yaml",
    "mv -f /opt/nitros-games/deploy.sh.candidate /opt/nitros-games/deploy.sh",
    "mv -f /opt/nitros-games/verify-restored-db.sh.candidate /opt/nitros-games/verify-restored-db.sh",
    (if $mode == "verify-restored-db" then "/opt/nitros-games/verify-restored-db.sh before " + ($region | @sh) else "true" end),
    ("/opt/nitros-games/deploy.sh " + ($image | @sh) + " " + ($region | @sh)),
    (if $mode == "verify-restored-db" then "/opt/nitros-games/verify-restored-db.sh after " + ($region | @sh) else "true" end),
    "cleanup",
    "trap - EXIT"
  ]}')"

command_id="$(aws ssm send-command \
  --instance-ids "$instance_id" \
  --document-name AWS-RunShellScript \
  --comment "Deploy immutable NitrosGames production image" \
  --timeout-seconds 900 \
  --parameters "$parameters" \
  --query Command.CommandId \
  --output text)"

status="Pending"
for _ in $(seq 1 180); do
  status="$(aws ssm get-command-invocation \
    --command-id "$command_id" \
    --instance-id "$instance_id" \
    --query Status \
    --output text 2>/dev/null || true)"
  case "$status" in
    Success|Cancelled|Failed|TimedOut) break ;;
  esac
  sleep 5
done

invocation="$(aws ssm get-command-invocation \
  --command-id "$command_id" \
  --instance-id "$instance_id" \
  --output json)"
remote_stdout="$(jq --raw-output '.StandardOutputContent' <<< "$invocation")"
deployment_result="$(sed -n 's/^DEPLOYMENT_RESULT=//p' <<< "$remote_stdout" | tail -n 1)"
deployed_image="$(sed -n 's/^DEPLOYED_IMAGE=//p' <<< "$remote_stdout" | tail -n 1)"
restored_image="$(sed -n 's/^RESTORED_IMAGE=//p' <<< "$remote_stdout" | tail -n 1)"
candidate_log="$(sed -n 's/^CANDIDATE_LOG=//p' <<< "$remote_stdout" | tail -n 1)"
flyway_phase="$(sed -n 's/^FLYWAY_PHASE=//p' <<< "$remote_stdout" | tail -n 1)"
flyway_version="$(sed -n 's/^FLYWAY_VERSION_OBSERVED=//p' <<< "$remote_stdout" | tail -n 1)"

echo "SSM_COMMAND_ID=$command_id"
echo "SSM_STATUS=${status:-unknown}"
echo "DEPLOYMENT_RESULT=${deployment_result:-unknown}"
[[ -z "$restored_image" ]] || echo "RESTORED_IMAGE=$restored_image"
[[ -z "$candidate_log" ]] || echo "CANDIDATE_LOG=$candidate_log"
[[ -z "$flyway_phase" ]] || echo "FLYWAY_PHASE=$flyway_phase"
[[ -z "$flyway_version" ]] || echo "FLYWAY_VERSION_OBSERVED=$flyway_version"

[[ "$status" == "Success" && "$deployment_result" == "success" && "$deployed_image" == "$app_image" ]] || {
  echo "Production deployment failed; inspect SSM command $command_id." >&2
  exit 1
}
if [[ "$deployment_mode" == "verify-restored-db" && "$flyway_phase" != "after" ]]; then
  echo "The post-start restored database verification did not complete." >&2
  exit 1
fi
