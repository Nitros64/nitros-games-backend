#!/usr/bin/env bash

# Shared production lifecycle safeguards. This file is sourced by GitHub
# Actions and deliberately performs no operation when it is loaded.

readonly PRODUCTION_PROJECT="nitros-games-backend"
readonly PRODUCTION_ENVIRONMENT="production"
readonly PRODUCTION_DB_IDENTIFIER="nitros-games-backend-production-mysql"
readonly PRODUCTION_ALB_NAME="nitros-games-backend-prod-api"
readonly PRODUCTION_STATE_PARAMETER="/nitros-games-backend/production/lifecycle/state"
readonly PRODUCTION_RELEASE_PARAMETER="/nitros-games-backend/production/lifecycle/release"
readonly PRODUCTION_API_URL="https://api.nitrosgames64.com"

production_require_full_sha() {
  local sha="${1:-}"
  [[ "$sha" =~ ^[0-9a-f]{40}$ ]] || {
    echo "A lowercase full 40-character commit SHA is required." >&2
    return 1
  }
}

production_require_hibernate_confirmation() {
  [[ "${1:-}" == "HIBERNATE_PRODUCTION" ]] || {
    echo "The exact HIBERNATE_PRODUCTION confirmation is required." >&2
    return 1
  }
}

production_validate_lifecycle_json() {
  local document="${1:-}"
  jq --exit-status '
    type == "object"
    and .schemaVersion == 1
    and (.desiredState == "ACTIVE" or .desiredState == "HIBERNATED")
    and (.lastTransitionRun == null or (.lastTransitionRun | type == "string"))
    and (.updatedAt | type == "string" and length > 0)
    and (keys | sort == ["desiredState", "lastTransitionRun", "schemaVersion", "updatedAt"])
  ' <<< "$document" >/dev/null
}

production_validate_release_json() {
  local document="${1:-}"
  jq --exit-status '
    type == "object"
    and .schemaVersion == 1
    and (.applicationSha | type == "string" and test("^[0-9a-f]{40}$"))
    and (.imageDigest | type == "string" and test("^sha256:[0-9a-f]{64}$"))
    and (.updatedAt | type == "string" and length > 0)
    and (keys | sort == ["applicationSha", "imageDigest", "schemaVersion", "updatedAt"])
  ' <<< "$document" >/dev/null
}

production_read_parameter() {
  local name="$1"
  aws ssm get-parameter \
    --name "$name" \
    --query 'Parameter.[Value,Version]' \
    --output json
}

production_read_lifecycle_state() {
  local response document
  response="$(production_read_parameter "$PRODUCTION_STATE_PARAMETER")"
  document="$(jq --exit-status --raw-output '.[0]' <<< "$response")"
  production_validate_lifecycle_json "$document" || {
    echo "The production lifecycle parameter is invalid." >&2
    return 1
  }
  printf '%s' "$document"
}

production_read_release_metadata() {
  local response document
  response="$(production_read_parameter "$PRODUCTION_RELEASE_PARAMETER")"
  document="$(jq --exit-status --raw-output '.[0]' <<< "$response")"
  production_validate_release_json "$document" || {
    echo "The production release metadata parameter is invalid." >&2
    return 1
  }
  printf '%s' "$document"
}

production_require_desired_state() {
  local expected="$1"
  local document="${2:-$(production_read_lifecycle_state)}"
  local actual
  actual="$(jq --raw-output '.desiredState' <<< "$document")"
  [[ "$actual" == "$expected" ]] || {
    echo "Production desiredState is $actual; expected $expected." >&2
    return 1
  }
}

production_require_snapshot_identifier() {
  [[ "${1:-}" =~ ^nitros-games-backend-production-hibernation(-final)?-[a-z0-9-]+$ ]] || {
    echo "The selected snapshot is outside the production hibernation namespace." >&2
    return 1
  }
}

production_restore_transition_marker() {
  local run_id="${1:-}"
  local snapshot_identifier="${2:-}"
  local application_sha="${3:-}"
  [[ "$run_id" =~ ^[0-9]+$ ]] || return 64
  production_require_snapshot_identifier "$snapshot_identifier"
  production_require_full_sha "$application_sha"
  printf 'restore:%s:%s:%s' "$run_id" "$snapshot_identifier" "$application_sha"
}

production_hibernate_transition_marker() {
  local run_id="${1:-}"
  [[ "$run_id" =~ ^[0-9]+$ ]] || return 64
  printf 'hibernate:%s' "$run_id"
}

production_require_resumable_restore() {
  local document="$1"
  local expected_snapshot="$2"
  local expected_sha="$3"
  local marker operation run_id snapshot_identifier application_sha extra
  production_validate_lifecycle_json "$document"
  production_require_desired_state ACTIVE "$document"
  marker="$(jq --raw-output '.lastTransitionRun // ""' <<< "$document")"
  IFS=: read -r operation run_id snapshot_identifier application_sha extra <<< "$marker"
  [[ "$operation" == "restore" && "$run_id" =~ ^[0-9]+$ && -z "${extra:-}" ]] || {
    echo "ACTIVE is not marked as an incomplete resumable restore." >&2
    return 1
  }
  [[ "$snapshot_identifier" == "$expected_snapshot" && "$application_sha" == "$expected_sha" ]] || {
    echo "Restore inputs differ from the in-progress transition." >&2
    return 1
  }
  production_require_snapshot_identifier "$snapshot_identifier"
  production_require_full_sha "$application_sha"
  printf '%s' "$run_id"
}

production_resolve_hibernate_operation_run() {
  local document="$1"
  local current_run_id="$2"
  local desired marker operation original_run extra
  production_validate_lifecycle_json "$document"
  [[ "$current_run_id" =~ ^[0-9]+$ ]] || return 64
  desired="$(jq --raw-output '.desiredState' <<< "$document")"
  if [[ "$desired" == "ACTIVE" ]]; then
    printf '%s' "$current_run_id"
    return
  fi
  marker="$(jq --raw-output '.lastTransitionRun // ""' <<< "$document")"
  IFS=: read -r operation original_run extra <<< "$marker"
  [[ "$desired" == "HIBERNATED" && "$operation" == "hibernate" && "$original_run" =~ ^[0-9]+$ && -z "${extra:-}" ]] || {
    echo "HIBERNATED is not marked as a resumable hibernation transition." >&2
    return 1
  }
  printf '%s' "$original_run"
}

production_resolve_start_action() {
  local document="$1"
  local actual_state="$2"
  local desired
  production_validate_lifecycle_json "$document"
  desired="$(jq --raw-output '.desiredState' <<< "$document")"
  if [[ "$desired" == "HIBERNATED" ]]; then
    echo "Production is HIBERNATED; run Restore Production instead." >&2
    return 1
  fi
  case "$actual_state" in
    STOPPED) printf 'START' ;;
    ACTIVE) printf 'VERIFY' ;;
    *)
      echo "Start cannot proceed from actual state $actual_state." >&2
      return 1
      ;;
  esac
}

production_resolve_stop_action() {
  local document="$1"
  local actual_state="$2"
  local desired
  production_validate_lifecycle_json "$document"
  desired="$(jq --raw-output '.desiredState' <<< "$document")"
  if [[ "$desired" == "HIBERNATED" ]]; then
    [[ "$actual_state" == "HIBERNATED" ]] || {
      echo "Hibernation is incomplete; resume Hibernate Production." >&2
      return 1
    }
    printf 'ALREADY_HIBERNATED'
    return
  fi
  case "$actual_state" in
    ACTIVE) printf 'STOP' ;;
    STOPPED) printf 'VERIFY_STOPPED' ;;
    *)
      echo "Stop cannot proceed from actual state $actual_state." >&2
      return 1
      ;;
  esac
}

production_transition_desired_state() {
  local expected="$1"
  local target="$2"
  local transition_run="$3"
  local now response current version updated confirmed confirmed_version

  [[ "$expected" =~ ^(ACTIVE|HIBERNATED)$ && "$target" =~ ^(ACTIVE|HIBERNATED)$ ]] || return 64
  [[ -n "$transition_run" ]] || return 64

  response="$(production_read_parameter "$PRODUCTION_STATE_PARAMETER")"
  current="$(jq --exit-status --raw-output '.[0]' <<< "$response")"
  version="$(jq --exit-status --raw-output '.[1]' <<< "$response")"
  production_validate_lifecycle_json "$current"
  production_require_desired_state "$expected" "$current"

  now="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  updated="$(jq --compact-output \
    --arg state "$target" \
    --arg run "$transition_run" \
    --arg now "$now" \
    '.desiredState = $state | .lastTransitionRun = $run | .updatedAt = $now' \
    <<< "$current")"
  production_validate_lifecycle_json "$updated"

  aws ssm put-parameter \
    --name "$PRODUCTION_STATE_PARAMETER" \
    --type String \
    --value "$updated" \
    --overwrite >/dev/null

  response="$(production_read_parameter "$PRODUCTION_STATE_PARAMETER")"
  confirmed="$(jq --exit-status --raw-output '.[0]' <<< "$response")"
  confirmed_version="$(jq --exit-status --raw-output '.[1]' <<< "$response")"
  [[ "$confirmed" == "$updated" && "$confirmed_version" -eq $((version + 1)) ]] || {
    echo "The production desired-state transition could not be verified." >&2
    return 1
  }
}

production_write_release_metadata() {
  local sha="$1"
  local digest="$2"
  local now document response confirmed
  production_require_full_sha "$sha"
  [[ "$digest" =~ ^sha256:[0-9a-f]{64}$ ]] || {
    echo "A valid immutable ECR digest is required." >&2
    return 1
  }

  now="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  document="$(jq --compact-output --null-input \
    --arg sha "$sha" \
    --arg digest "$digest" \
    --arg now "$now" \
    '{schemaVersion: 1, applicationSha: $sha, imageDigest: $digest, updatedAt: $now}')"
  production_validate_release_json "$document"
  aws ssm put-parameter \
    --name "$PRODUCTION_RELEASE_PARAMETER" \
    --type String \
    --value "$document" \
    --overwrite >/dev/null
  response="$(production_read_parameter "$PRODUCTION_RELEASE_PARAMETER")"
  confirmed="$(jq --exit-status --raw-output '.[0]' <<< "$response")"
  [[ "$confirmed" == "$document" ]] || {
    echo "The production release metadata update could not be verified." >&2
    return 1
  }
}

production_discover_instance_ids() {
  aws ec2 describe-instances \
    --filters \
      "Name=tag:Name,Values=nitros-games-backend-production-application" \
      "Name=tag:Project,Values=$PRODUCTION_PROJECT" \
      "Name=tag:Environment,Values=$PRODUCTION_ENVIRONMENT" \
      "Name=tag:Component,Values=production-runtime" \
      "Name=instance-state-name,Values=pending,running,stopping,stopped" \
    --query 'Reservations[].Instances[].InstanceId' \
    --output text \
    | tr '\t' '\n' \
    | sed '/^$/d;/^None$/d' \
    | sort -u
}

production_require_single_instance() {
  local ids count
  ids="$(production_discover_instance_ids)"
  count="$(sed '/^$/d' <<< "$ids" | wc -l | tr -d ' ')"
  [[ "$count" -eq 1 ]] || {
    echo "Expected exactly one tagged production EC2 instance; found $count." >&2
    return 1
  }
  printf '%s' "$ids"
}

production_instance_state() {
  local instance_id="$1"
  aws ec2 describe-instances \
    --instance-ids "$instance_id" \
    --query 'Reservations[0].Instances[0].State.Name' \
    --output text
}

production_database_status() {
  local status
  status="$(aws rds describe-db-instances \
    --db-instance-identifier "$PRODUCTION_DB_IDENTIFIER" \
    --query 'DBInstances[0].DBInstanceStatus' \
    --output text 2>/dev/null || true)"
  [[ -n "$status" && "$status" != "None" ]] && printf '%s' "$status" || printf 'ABSENT'
}

production_load_balancer_status() {
  if aws elbv2 describe-load-balancers --names "$PRODUCTION_ALB_NAME" >/dev/null 2>&1; then
    printf 'PRESENT'
  else
    printf 'ABSENT'
  fi
}

production_detect_state() {
  local lifecycle desired ids count db_status alb_status ec2_state
  lifecycle="$(production_read_lifecycle_state)"
  desired="$(jq --raw-output '.desiredState' <<< "$lifecycle")"
  ids="$(production_discover_instance_ids)"
  count="$(sed '/^$/d' <<< "$ids" | wc -l | tr -d ' ')"
  db_status="$(production_database_status)"
  alb_status="$(production_load_balancer_status)"

  if [[ "$desired" == "HIBERNATED" && "$count" -eq 0 && "$db_status" == "ABSENT" && "$alb_status" == "ABSENT" ]]; then
    printf 'HIBERNATED'
    return
  fi
  if [[ "$count" -ne 1 ]]; then
    printf 'INCONSISTENT'
    return
  fi
  ec2_state="$(production_instance_state "$ids")"
  if [[ "$desired" == "ACTIVE" && "$ec2_state" == "running" && "$db_status" == "available" && "$alb_status" == "PRESENT" ]]; then
    printf 'ACTIVE'
  elif [[ "$desired" == "ACTIVE" && "$ec2_state" == "stopped" && ( "$db_status" == "stopped" || "$db_status" == "available" ) && "$alb_status" == "PRESENT" ]]; then
    printf 'STOPPED'
  elif [[ "$ec2_state" =~ ^(pending|stopping)$ || "$db_status" =~ ^(starting|stopping|creating|backing-up|modifying|rebooting|deleting)$ ]]; then
    printf 'TRANSITIONING'
  else
    printf 'INCONSISTENT'
  fi
}

production_wait_for_database_status() {
  local expected="$1"
  local attempts="${2:-120}"
  local status
  for _ in $(seq 1 "$attempts"); do
    status="$(production_database_status)"
    [[ "$status" == "$expected" ]] && return 0
    sleep 15
  done
  echo "RDS did not reach $expected (last status: $status)." >&2
  return 1
}

production_wait_for_instance_state() {
  local instance_id="$1"
  local expected="$2"
  local attempts="${3:-80}"
  local status
  for _ in $(seq 1 "$attempts"); do
    status="$(production_instance_state "$instance_id")"
    [[ "$status" == "$expected" ]] && return 0
    sleep 15
  done
  echo "EC2 $instance_id did not reach $expected (last status: $status)." >&2
  return 1
}

production_wait_for_ssm_online() {
  local instance_id="$1"
  local attempts="${2:-60}"
  local status
  for _ in $(seq 1 "$attempts"); do
    status="$(aws ssm describe-instance-information \
      --filters "Key=InstanceIds,Values=$instance_id" \
      --query 'InstanceInformationList[0].PingStatus' \
      --output text 2>/dev/null || true)"
    [[ "$status" == "Online" ]] && return 0
    sleep 10
  done
  echo "EC2 $instance_id did not become Online in SSM (last status: ${status:-unknown})." >&2
  return 1
}

production_wait_for_host_bootstrap() {
  local instance_id="$1"
  local parameters command_id status
  parameters='{"commands":["set -euo pipefail","for attempt in $(seq 1 90); do test -f /var/lib/nitros-games-bootstrap-ready && exit 0; sleep 10; done","echo Production host bootstrap did not complete. >&2","exit 1"]}'
  command_id="$(aws ssm send-command \
    --instance-ids "$instance_id" \
    --document-name AWS-RunShellScript \
    --comment "Wait for NitrosGames production host bootstrap" \
    --timeout-seconds 920 \
    --parameters "$parameters" \
    --query Command.CommandId \
    --output text)"
  for _ in $(seq 1 100); do
    status="$(aws ssm get-command-invocation \
      --command-id "$command_id" --instance-id "$instance_id" \
      --query Status --output text 2>/dev/null || true)"
    case "$status" in
      Success) return 0 ;;
      Cancelled|Failed|TimedOut) break ;;
    esac
    sleep 10
  done
  echo "Production host bootstrap failed or timed out (SSM: ${status:-unknown})." >&2
  return 1
}

production_require_ecr_image() {
  local repository="$1"
  local sha="$2"
  local digest
  production_require_full_sha "$sha"
  digest="$(aws ecr describe-images \
    --repository-name "$repository" \
    --image-ids "imageTag=$sha" \
    --query 'imageDetails[0].imageDigest' \
    --output text 2>/dev/null || true)"
  [[ "$digest" =~ ^sha256:[0-9a-f]{64}$ ]] || {
    echo "The immutable ECR image for $sha does not exist." >&2
    return 1
  }
  printf '%s' "$digest"
}

production_validate_snapshot() {
  local snapshot_identifier="$1"
  local snapshot engine version family
  production_require_snapshot_identifier "$snapshot_identifier"
  snapshot="$(aws rds describe-db-snapshots \
    --db-snapshot-identifier "$snapshot_identifier" \
    --query 'DBSnapshots[0]' \
    --output json)"
  engine="$(jq --raw-output '.Engine' <<< "$snapshot")"
  version="$(jq --raw-output '.EngineVersion' <<< "$snapshot")"
  [[ "$(jq --raw-output '.Status' <<< "$snapshot")" == "available" ]]
  [[ "$(jq --raw-output '.Encrypted' <<< "$snapshot")" == "true" ]]
  [[ "$engine" == "mysql" && "$version" =~ ^8\.4\.[0-9]+$ ]]
  [[ "$(jq --raw-output '.DBName' <<< "$snapshot")" == "nitrosgames" ]]
  [[ "$(jq --raw-output '.MasterUsername' <<< "$snapshot")" == "nitros_admin" ]]
  [[ "$(jq --raw-output '.StorageType' <<< "$snapshot")" == "gp3" ]]
  [[ "$(jq --raw-output '.AllocatedStorage' <<< "$snapshot")" -le 100 ]]
  family="$(aws rds describe-db-engine-versions \
    --engine "$engine" \
    --engine-version "$version" \
    --query 'DBEngineVersions[0].DBParameterGroupFamily' \
    --output text)"
  [[ "$family" == "mysql8.4" ]] || {
    echo "Snapshot $snapshot_identifier is not compatible with mysql8.4." >&2
    return 1
  }
  printf '%s' "$version"
}

production_plan_change_lines() {
  local plan_json="$1"
  jq --raw-output '
    .resource_changes[]?
    | select(.change.actions != ["no-op"])
    | [.address, (.change.actions | join(","))]
    | @tsv
  ' "$plan_json" | tr -d '\r'
}

production_runtime_address_allowed() {
  case "$1" in
    aws_instance.application\[0\]) return 0 ;;
    aws_iam_role.application\[0\]) return 0 ;;
    aws_iam_role_policy_attachment.ssm_core\[0\]) return 0 ;;
    aws_iam_role_policy.ecr_pull\[0\]) return 0 ;;
    aws_iam_role_policy.host_images\[0\]) return 0 ;;
    aws_iam_role_policy.application_database_secret\[0\]) return 0 ;;
    aws_iam_instance_profile.application\[0\]) return 0 ;;
    aws_iam_role.github_production_deployer\[0\]) return 0 ;;
    aws_iam_role_policy.github_production_deploy\[0\]) return 0 ;;
    aws_security_group.load_balancer\[0\]) return 0 ;;
    aws_vpc_security_group_ingress_rule.load_balancer_https_ipv4\[0\]) return 0 ;;
    aws_vpc_security_group_ingress_rule.load_balancer_http_redirect_ipv4\[0\]) return 0 ;;
    aws_vpc_security_group_egress_rule.load_balancer_to_application\[0\]) return 0 ;;
    aws_vpc_security_group_ingress_rule.application_from_load_balancer\[0\]) return 0 ;;
    aws_lb.application\[0\]) return 0 ;;
    aws_lb_target_group.application\[0\]) return 0 ;;
    aws_lb_target_group_attachment.application\[0\]) return 0 ;;
    aws_acm_certificate.api\[0\]) return 0 ;;
    aws_acm_certificate_validation.api\[0\]) return 0 ;;
    aws_lb_listener.https\[0\]) return 0 ;;
    aws_lb_listener.http_redirect\[0\]) return 0 ;;
    aws_route53_record.api\[0\]) return 0 ;;
    aws_route53_record.api_certificate_validation\[\"api.nitrosgames64.com\"\]) return 0 ;;
    *) return 1 ;;
  esac
}

production_require_plan_mode() {
  local mode="$1"
  local plan_json="$2"
  local expected_final_snapshot="${3:-}"
  local lines count address actions
  lines="$(production_plan_change_lines "$plan_json")"
  count="$(sed '/^$/d' <<< "$lines" | wc -l | tr -d ' ')"

  case "$mode" in
    stable|normalized-data)
      [[ "$count" -eq 0 ]] || {
        echo "Expected a no-change $mode plan, found:" >&2
        printf '%s\n' "$lines" >&2
        return 1
      }
      ;;
    restore-data|converge-restore-data)
      if [[ "$mode" == "converge-restore-data" && "$count" -eq 0 ]]; then
        return 0
      fi
      [[ "$count" -eq 1 && "$lines" == $'aws_db_instance.mysql[0]\tcreate' ]] || return 1
      ;;
    protect-data|converge-protect-data)
      if [[ "$mode" == "converge-protect-data" && "$count" -eq 0 ]]; then
        return 0
      fi
      [[ "$count" -eq 1 && "$lines" == $'aws_db_instance.mysql[0]\tupdate' ]] || return 1
      jq --exit-status --arg expected_final_snapshot "$expected_final_snapshot" '
        .resource_changes[]
        | select(.address == "aws_db_instance.mysql[0]")
        | .change as $change
        | ($change.before // {}) as $before
        | ($change.after // {}) as $after
        | [
            (($before | keys_unsorted) + ($after | keys_unsorted) | unique[])
            | select($before[.] != $after[.])
          ] as $changed
        | ($changed | length) > 0
          and all($changed[]; . == "deletion_protection" or . == "final_snapshot_identifier")
          and $after.deletion_protection == false
          and ($expected_final_snapshot == "" or $after.final_snapshot_identifier == $expected_final_snapshot)
          and (if ($changed | index("deletion_protection")) != null
               then $before.deletion_protection == true
               else true
               end)
      ' "$plan_json" >/dev/null || {
        echo "RDS protection plan changed attributes outside the approved hibernation transition." >&2
        return 1
      }
      ;;
    delete-data|converge-delete-data)
      if [[ "$mode" == "converge-delete-data" && "$count" -eq 0 ]]; then
        return 0
      fi
      [[ "$count" -eq 1 && "$lines" == $'aws_db_instance.mysql[0]\tdelete' ]] || return 1
      ;;
    restore-runtime|delete-runtime|converge-restore-runtime|converge-delete-runtime)
      local expected_action="create"
      [[ "$mode" == "delete-runtime" || "$mode" == "converge-delete-runtime" ]] && expected_action="delete"
      if [[ "$mode" == converge-* && "$count" -eq 0 ]]; then
        return 0
      fi
      if [[ "$mode" == converge-* ]]; then
        [[ "$count" -le 23 ]] || return 1
      else
        [[ "$count" -eq 23 ]] || {
          echo "Expected exactly 23 runtime changes, found $count." >&2
          return 1
        }
      fi
      while IFS=$'\t' read -r address actions; do
        [[ "$actions" == "$expected_action" ]] || return 1
        production_runtime_address_allowed "$address" || {
          echo "Unexpected runtime plan address: $address" >&2
          return 1
        }
      done <<< "$lines"
      ;;
    *)
      echo "Unknown production plan mode: $mode" >&2
      return 64
      ;;
  esac
}
