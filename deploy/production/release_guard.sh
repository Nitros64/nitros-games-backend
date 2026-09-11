#!/usr/bin/env bash

production_cd_require_main_ref() {
  local ref="${1:-}"
  [[ "$ref" == "refs/heads/main" ]] || {
    echo "Production CD must be dispatched from the main branch." >&2
    return 64
  }
}

production_cd_require_full_sha() {
  local sha="${1:-}"
  [[ "$sha" =~ ^[0-9a-f]{40}$ ]] || {
    echo "commit_sha must be a full lowercase 40-character Git SHA." >&2
    return 64
  }
}

production_cd_require_main_membership() {
  local comparison="${1:-}"
  [[ "$comparison" == "ahead" || "$comparison" == "identical" ]] || {
    echo "The requested commit is not part of main." >&2
    return 65
  }
}

production_cd_require_successful_ci() {
  local conclusion="${1:-}"
  [[ "$conclusion" == "success" ]] || {
    echo "The requested commit does not have a successful main CI run." >&2
    return 66
  }
}

production_cd_require_ecr_image() {
  local digest="${1:-}"
  [[ "$digest" =~ ^sha256:[0-9a-f]{64}$ ]] || {
    echo "The immutable ECR image for the requested commit does not exist." >&2
    return 67
  }
}

production_cd_require_ssm_success() {
  local status="${1:-}"
  [[ "$status" == "Success" ]] || {
    echo "The production SSM deployment did not complete successfully." >&2
    return 68
  }
}

production_cd_require_deployment_success() {
  local result="${1:-}"
  [[ "$result" == "success" ]] || {
    echo "The production runtime did not report DEPLOYMENT_RESULT=success." >&2
    return 69
  }
}

production_cd_require_http_200() {
  local label="${1:-HTTP request}"
  local status="${2:-}"
  [[ "$status" == "200" ]] || {
    echo "$label returned HTTP ${status:-unknown}; expected 200." >&2
    return 70
  }
}
