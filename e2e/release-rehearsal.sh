#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$SCRIPT_DIRECTORY/.." && pwd)"
readonly STATE_FILE="${E2E_STATE_FILE:-${TMPDIR:-/tmp}/nitros-games-release-rehearsal-state.json}"
readonly API_BASE_URL="${E2E_API_BASE_URL:-http://localhost:8080}"
readonly TOKEN_URL="${E2E_TOKEN_URL:-http://localhost:8081/realms/nitros-games/protocol/openid-connect/token}"
readonly ADMIN_CLIENT_ID="nitros-games-cli"
readonly READER_CLIENT_ID="nitros-games-reader-cli"

RESPONSE_BODY=""
HTTP_STATUS=""
TEMPORARY_IMAGE=""

cd "$REPOSITORY_ROOT"

log() {
  printf '[e2e] %s\n' "$*"
}

fail() {
  printf '[e2e] ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

read_dotenv_value() {
  local key="$1"
  [[ -f .env ]] || return 0
  awk -F= -v key="$key" '$1 == key {sub(/^[^=]*=/, ""); value=$0} END {print value}' .env \
    | tr -d '\r'
}

require_secret() {
  local variable_name="$1"
  local value="$2"
  [[ -n "$value" ]] || fail "$variable_name is not set in the environment or .env"
  [[ "$value" != replace-* && "$value" != change-me ]] \
    || fail "$variable_name still contains an example placeholder"
}

cleanup_temporary_files() {
  if [[ -n "$TEMPORARY_IMAGE" && -f "$TEMPORARY_IMAGE" ]]; then
    rm -f -- "$TEMPORARY_IMAGE"
  fi
}
trap cleanup_temporary_files EXIT

for command_name in curl jq docker base64 awk; do
  require_command "$command_name"
done
docker compose version >/dev/null 2>&1 || fail "Docker Compose is not available"

ADMIN_CLIENT_SECRET="${NITROS_GAMES_CLI_SECRET:-$(read_dotenv_value NITROS_GAMES_CLI_SECRET)}"
READER_CLIENT_SECRET="${NITROS_GAMES_READER_CLI_SECRET:-$(read_dotenv_value NITROS_GAMES_READER_CLI_SECRET)}"
require_secret NITROS_GAMES_CLI_SECRET "$ADMIN_CLIENT_SECRET"
require_secret NITROS_GAMES_READER_CLI_SECRET "$READER_CLIENT_SECRET"

request() {
  local label="$1"
  local method="$2"
  local path="$3"
  local expected_status="$4"
  local token="$5"
  shift 5

  local response_file
  response_file="$(mktemp)"
  local -a curl_arguments=(
    --silent
    --show-error
    --output "$response_file"
    --write-out '%{http_code}'
    --request "$method"
  )
  if [[ -n "$token" ]]; then
    curl_arguments+=(--header "Authorization: Bearer $token")
  fi
  curl_arguments+=("$@" "${API_BASE_URL}${path}")

  if ! HTTP_STATUS="$(curl "${curl_arguments[@]}")"; then
    rm -f -- "$response_file"
    fail "$label could not reach ${API_BASE_URL}${path}"
  fi
  RESPONSE_BODY="$(<"$response_file")"
  rm -f -- "$response_file"

  if [[ "$HTTP_STATUS" != "$expected_status" ]]; then
    printf '[e2e] Response body: %s\n' "$RESPONSE_BODY" >&2
    fail "$label expected HTTP $expected_status but received $HTTP_STATUS"
  fi
  log "PASS $label (HTTP $HTTP_STATUS)"
}

request_json() {
  local label="$1"
  local method="$2"
  local path="$3"
  local expected_status="$4"
  local token="$5"
  local body="$6"
  request "$label" "$method" "$path" "$expected_status" "$token" \
    --header 'Content-Type: application/json' \
    --data "$body"
}

assert_response() {
  local expression="$1"
  local description="$2"
  if ! jq --exit-status "$expression" >/dev/null <<<"$RESPONSE_BODY"; then
    printf '[e2e] Response body: %s\n' "$RESPONSE_BODY" >&2
    fail "$description"
  fi
}

response_id() {
  local id
  id="$(jq --exit-status --raw-output '.id | select(type == "number")' <<<"$RESPONSE_BODY")" \
    || fail "A successful creation response did not contain a numeric id"
  [[ "$id" =~ ^[1-9][0-9]*$ ]] || fail "Invalid resource id returned by the API: $id"
  printf '%s' "$id"
}

obtain_token() {
  local client_id="$1"
  local client_secret="$2"
  local token_response
  token_response="$(curl --fail --silent --show-error \
    --request POST \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=client_credentials' \
    --data-urlencode "client_id=$client_id" \
    --data-urlencode "client_secret=$client_secret" \
    "$TOKEN_URL")" || fail "Could not obtain a token for $client_id"
  jq --exit-status --raw-output '.access_token | select(type == "string" and length > 0)' \
    <<<"$token_response" || fail "Keycloak did not return an access token for $client_id"
}

initialize_state() {
  [[ ! -e "$STATE_FILE" ]] \
    || fail "State already exists at $STATE_FILE; run cleanup or select another E2E_STATE_FILE"

  local timestamp alpha_suffix run_id temporary_state
  timestamp="$(date -u +%Y%m%d%H%M%S)"
  alpha_suffix="$(printf '%s' "${timestamp: -8}" | tr '0-9' 'abcdefghij')"
  run_id="e2e-${timestamp}-$$"
  temporary_state="$(mktemp)"

  jq --null-input \
    --arg runId "$run_id" \
    --arg genreName "Genre${alpha_suffix}" \
    --arg platformName "Platform${alpha_suffix}" \
    --arg processorName "p${timestamp: -8}" \
    --arg languageName "L${alpha_suffix}" \
    --arg toolTypeName "Type${alpha_suffix}" \
    --arg toolName "Tool${alpha_suffix}" \
    --arg toolWebPage "https://example.test/tools/${run_id}" \
    --arg toolImagePath "tool-${alpha_suffix}.png" \
    --arg hostImageName "Host${alpha_suffix}" \
    --arg gameName "Game${timestamp}" \
    --arg versionName "Version${timestamp}" \
    --arg downloadLink "https://example.test/downloads/${run_id}.zip" \
    '{
      runId: $runId,
      names: {
        genre: $genreName,
        platform: $platformName,
        processor: $processorName,
        language: $languageName,
        toolType: $toolTypeName,
        tool: $toolName,
        toolWebPage: $toolWebPage,
        toolImagePath: $toolImagePath,
        hostImage: $hostImageName,
        game: $gameName,
        version: $versionName,
        downloadLink: $downloadLink
      },
      ids: {
        genre: null,
        platform: null,
        processor: null,
        language: null,
        toolType: null,
        tool: null,
        hostImage: null,
        game: null,
        version: null,
        downloadLink: null
      },
      fixture: {toolLanguageCreated: false}
    }' >"$temporary_state"
  chmod 0600 "$temporary_state"
  mv "$temporary_state" "$STATE_FILE"
  log "Run ID: $run_id"
  log "State: $STATE_FILE"
}

state_value() {
  jq --raw-output "$1" "$STATE_FILE"
}

state_set_id() {
  local key="$1"
  local value="$2"
  [[ "$value" =~ ^[1-9][0-9]*$ ]] || fail "Refusing to persist invalid id for $key"
  local temporary_state
  temporary_state="$(mktemp)"
  jq --argjson value "$value" ".ids.${key} = \$value" "$STATE_FILE" >"$temporary_state"
  chmod 0600 "$temporary_state"
  mv "$temporary_state" "$STATE_FILE"
}

state_mark_fixture_created() {
  local temporary_state
  temporary_state="$(mktemp)"
  jq '.fixture.toolLanguageCreated = true' "$STATE_FILE" >"$temporary_state"
  chmod 0600 "$temporary_state"
  mv "$temporary_state" "$STATE_FILE"
}

mysql_sql() {
  local sql="$1"
  printf '%s\n' "$sql" | docker compose exec --no-TTY mysql sh -c \
    'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --batch --skip-column-names --user="$MYSQL_USER" "$MYSQL_DATABASE"'
}

create_tool_language_fixture() {
  local language_id="$1"
  local tool_id="$2"
  [[ "$language_id" =~ ^[1-9][0-9]*$ && "$tool_id" =~ ^[1-9][0-9]*$ ]] \
    || fail "The tool_lang fixture requires numeric API-created ids"

  log "SETUP E2E FIXTURE tool_lang ($language_id, $tool_id)"
  mysql_sql "INSERT INTO tool_lang (program_lang_id, program_tool_id) VALUES ($language_id, $tool_id);"
  state_mark_fixture_created
  local count
  count="$(mysql_sql "SELECT COUNT(*) FROM tool_lang WHERE program_lang_id=$language_id AND program_tool_id=$tool_id;")"
  [[ "$count" == "1" ]] || fail "The tool_lang E2E fixture was not created"
  log "PASS controlled tool_lang fixture"
}

remove_tool_language_fixture() {
  local language_id="$1"
  local tool_id="$2"
  [[ "$language_id" =~ ^[1-9][0-9]*$ && "$tool_id" =~ ^[1-9][0-9]*$ ]] \
    || fail "Refusing to remove tool_lang without exact numeric ids"
  log "CLEANUP E2E FIXTURE tool_lang ($language_id, $tool_id)"
  mysql_sql "DELETE FROM tool_lang WHERE program_lang_id=$language_id AND program_tool_id=$tool_id;"
}

create_phase() {
  initialize_state

  local reader_token admin_token
  reader_token="$(obtain_token "$READER_CLIENT_ID" "$READER_CLIENT_SECRET")"
  admin_token="$(obtain_token "$ADMIN_CLIENT_ID" "$ADMIN_CLIENT_SECRET")"

  request "readiness" GET "/actuator/health/readiness" 200 ""
  assert_response '.status == "UP"' "Readiness response was not UP"

  local genre_name
  genre_name="$(state_value '.names.genre')"
  request_json "mutation without JWT" POST "/api/v1/game-genres" 401 "" \
    "{\"name\":\"$genre_name\"}"
  assert_response '.code == "authentication_required"' "Unauthenticated mutation did not use the expected problem code"

  request_json "mutation with non-ADMIN JWT" POST "/api/v1/game-genres" 403 "$reader_token" \
    "{\"name\":\"$genre_name\"}"
  assert_response '.code == "access_denied"' "Reader mutation did not use the expected forbidden problem code"

  request "valid ADMIN JWT" GET "/actuator/prometheus" 200 "$admin_token"

  request_json "create genre" POST "/api/v1/game-genres" 201 "$admin_token" \
    "{\"name\":\"$genre_name\"}"
  local genre_id
  genre_id="$(response_id)"
  state_set_id genre "$genre_id"

  local platform_name
  platform_name="$(state_value '.names.platform')"
  request_json "create platform" POST "/api/v1/platforms" 201 "$admin_token" \
    "{\"name\":\"$platform_name\"}"
  local platform_id
  platform_id="$(response_id)"
  state_set_id platform "$platform_id"

  local processor_name
  processor_name="$(state_value '.names.processor')"
  request_json "create processor" POST "/api/v1/processors" 201 "$admin_token" \
    "{\"name\":\"$processor_name\"}"
  local processor_id
  processor_id="$(response_id)"
  state_set_id processor "$processor_id"

  local language_name
  language_name="$(state_value '.names.language')"
  request_json "create programming language" POST "/api/v1/programming-languages" 201 "$admin_token" \
    "{\"name\":\"$language_name\"}"
  local language_id
  language_id="$(response_id)"
  state_set_id language "$language_id"

  local tool_type_name
  tool_type_name="$(state_value '.names.toolType')"
  request_json "create programming tool type" POST "/api/v1/programming-tool-types" 201 "$admin_token" \
    "{\"name\":\"$tool_type_name\"}"
  local tool_type_id
  tool_type_id="$(response_id)"
  state_set_id toolType "$tool_type_id"

  local tool_name tool_web_page tool_image_path
  tool_name="$(state_value '.names.tool')"
  tool_web_page="$(state_value '.names.toolWebPage')"
  tool_image_path="$(state_value '.names.toolImagePath')"
  request_json "create programming tool" POST "/api/v1/programming-tools" 201 "$admin_token" \
    "$(jq --null-input --compact-output \
      --arg name "$tool_name" \
      --arg webPage "$tool_web_page" \
      --arg imagefilePath "$tool_image_path" \
      --argjson toolTypeId "$tool_type_id" \
      '{name:$name, webPage:$webPage, imagefilePath:$imagefilePath, toolTypeId:$toolTypeId}')"
  local tool_id
  tool_id="$(response_id)"
  state_set_id tool "$tool_id"

  create_tool_language_fixture "$language_id" "$tool_id"

  TEMPORARY_IMAGE="$(mktemp --suffix=.png)"
  printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=' \
    | base64 --decode >"$TEMPORARY_IMAGE"
  local curl_image_path="$TEMPORARY_IMAGE"
  if command -v cygpath >/dev/null 2>&1; then
    curl_image_path="$(cygpath --windows "$TEMPORARY_IMAGE")"
  fi
  local host_image_name
  host_image_name="$(state_value '.names.hostImage')"
  request "upload host image" POST "/api/v1/server-host-images" 201 "$admin_token" \
    --form "name=$host_image_name" \
    --form "fileHostImage=@${curl_image_path};type=image/png"
  local host_image_id
  host_image_id="$(response_id)"
  state_set_id hostImage "$host_image_id"

  local game_name
  game_name="$(state_value '.names.game')"
  request_json "create game" POST "/api/v1/games" 201 "$admin_token" \
    "$(jq --null-input --compact-output \
      --arg name "$game_name" \
      --argjson genreId "$genre_id" \
      '{name:$name, description:"E2E release rehearsal", jam:false, developerCount:2, genreIds:[$genreId]}')"
  local game_id
  game_id="$(response_id)"
  state_set_id game "$game_id"

  local version_name
  version_name="$(state_value '.names.version')"
  request_json "create game version" POST "/api/v1/games/${game_id}/versions" 201 "$admin_token" \
    "$(jq --null-input --compact-output \
      --arg name "$version_name" \
      --argjson programmingLanguageId "$language_id" \
      --argjson programmingToolId "$tool_id" \
      --argjson platformId "$platform_id" \
      --argjson processorId "$processor_id" \
      '{name:$name, programmingLanguageId:$programmingLanguageId, programmingToolId:$programmingToolId, platformId:$platformId, processorId:$processorId}')"
  local version_id
  version_id="$(response_id)"
  state_set_id version "$version_id"

  local download_link
  download_link="$(state_value '.names.downloadLink')"
  local download_payload
  download_payload="$(jq --null-input --compact-output \
    --arg link "$download_link" \
    --argjson serverHostImageId "$host_image_id" \
    '{link:$link, serverHostImageId:$serverHostImageId}')"
  request_json "create download link" POST \
    "/api/v1/games/${game_id}/versions/${version_id}/download-links" 201 "$admin_token" \
    "$download_payload"
  local download_link_id
  download_link_id="$(response_id)"
  state_set_id downloadLink "$download_link_id"

  verify_resources

  request "search and bounded pagination" GET \
    "/api/v1/games/search?name=${game_name}&genreId=${genre_id}&jam=false&page=0&size=500&sort=name,asc" \
    200 ""
  if ! jq --exit-status --argjson id "$game_id" '.content | any(.id == $id)' \
      >/dev/null <<<"$RESPONSE_BODY"; then
    printf '[e2e] Response body: %s\n' "$RESPONSE_BODY" >&2
    fail "The created game was not returned by the combined search"
  fi
  assert_response '.size == 100' "The configured page-size limit was not enforced"

  request "representative validation error" GET "/api/v1/games/search?genreId=0" 400 ""
  assert_response '.code == "validation_failed"' "The 400 response did not use validation_failed"

  request "representative missing resource" GET "/api/v1/games/9223372036854775807" 404 ""
  assert_response '.code == "resource_not_found"' "The 404 response did not use resource_not_found"

  request_json "representative data conflict" POST \
    "/api/v1/games/${game_id}/versions/${version_id}/download-links" 409 "$admin_token" \
    "$download_payload"
  assert_response '.code == "data_conflict"' "The 409 response did not use data_conflict"

  log "CREATE COMPLETE"
  log "Next: ./e2e/release-rehearsal.sh verify"
}

assert_resource_id() {
  local label="$1"
  local path="$2"
  local expected_id="$3"
  request "$label" GET "$path" 200 ""
  if ! jq --exit-status --argjson id "$expected_id" '.id == $id' >/dev/null <<<"$RESPONSE_BODY"; then
    printf '[e2e] Response body: %s\n' "$RESPONSE_BODY" >&2
    fail "$label returned a different resource"
  fi
}

verify_resources() {
  [[ -f "$STATE_FILE" ]] || fail "State not found at $STATE_FILE; run create first"
  jq --exit-status '.ids | all(. != null)' "$STATE_FILE" >/dev/null \
    || fail "State is incomplete; cleanup may still be used for resources already recorded"

  request "readiness during verification" GET "/actuator/health/readiness" 200 ""
  assert_response '.status == "UP"' "Readiness response was not UP"

  local genre_id platform_id processor_id language_id tool_type_id tool_id
  local host_image_id game_id version_id download_link_id
  genre_id="$(state_value '.ids.genre')"
  platform_id="$(state_value '.ids.platform')"
  processor_id="$(state_value '.ids.processor')"
  language_id="$(state_value '.ids.language')"
  tool_type_id="$(state_value '.ids.toolType')"
  tool_id="$(state_value '.ids.tool')"
  host_image_id="$(state_value '.ids.hostImage')"
  game_id="$(state_value '.ids.game')"
  version_id="$(state_value '.ids.version')"
  download_link_id="$(state_value '.ids.downloadLink')"

  assert_resource_id "read back genre" "/api/v1/game-genres/$genre_id" "$genre_id"
  assert_resource_id "read back platform" "/api/v1/platforms/$platform_id" "$platform_id"
  assert_resource_id "read back processor" "/api/v1/processors/$processor_id" "$processor_id"
  assert_resource_id "read back language" "/api/v1/programming-languages/$language_id" "$language_id"
  assert_resource_id "read back tool type" "/api/v1/programming-tool-types/$tool_type_id" "$tool_type_id"
  assert_resource_id "read back tool" "/api/v1/programming-tools/$tool_id" "$tool_id"
  assert_resource_id "read back host image metadata" "/api/v1/server-host-images/$host_image_id" "$host_image_id"
  assert_resource_id "read back game" "/api/v1/games/$game_id" "$game_id"
  assert_resource_id "read back game version" "/api/v1/games/$game_id/versions/$version_id" "$version_id"
  assert_resource_id "read back download link" \
    "/api/v1/games/$game_id/versions/$version_id/download-links/$download_link_id" \
    "$download_link_id"

  local language_tool_count
  language_tool_count="$(mysql_sql \
    "SELECT COUNT(*) FROM tool_lang WHERE program_lang_id=$language_id AND program_tool_id=$tool_id;")"
  [[ "$language_tool_count" == "1" ]] || fail "The exact tool_lang fixture did not survive"
  log "PASS controlled tool_lang fixture persisted"

  local image_path
  request "obtain persisted host image path" GET "/api/v1/server-host-images/$host_image_id" 200 ""
  image_path="$(jq --exit-status --raw-output '.imagepath | select(type == "string" and length > 0)' \
    <<<"$RESPONSE_BODY")" || fail "Host image metadata has no imagepath"
  MSYS_NO_PATHCONV=1 docker compose exec --no-TTY api \
    test -f "/var/lib/nitros-games/host-images/$image_path" \
    || fail "Uploaded host image bytes are missing from the volume"
  log "PASS uploaded image bytes persisted"
}

verify_phase() {
  local admin_token
  admin_token="$(obtain_token "$ADMIN_CLIENT_ID" "$ADMIN_CLIENT_SECRET")"
  request "valid ADMIN JWT during verification" GET "/actuator/prometheus" 200 "$admin_token"
  verify_resources
  log "VERIFY COMPLETE for $(state_value '.runId')"
}

delete_resource() {
  local label="$1"
  local path="$2"
  local token="$3"
  local response_file status
  response_file="$(mktemp)"
  status="$(curl --silent --show-error \
    --output "$response_file" \
    --write-out '%{http_code}' \
    --request DELETE \
    --header "Authorization: Bearer $token" \
    "${API_BASE_URL}${path}")" || {
      rm -f -- "$response_file"
      fail "$label could not reach the API"
    }
  RESPONSE_BODY="$(<"$response_file")"
  rm -f -- "$response_file"
  if [[ "$status" != "204" && "$status" != "404" ]]; then
    printf '[e2e] Response body: %s\n' "$RESPONSE_BODY" >&2
    fail "$label expected HTTP 204 or 404 but received $status"
  fi
  log "PASS $label (HTTP $status)"
}

recorded_id() {
  local key="$1"
  jq --raw-output ".ids.${key} // empty" "$STATE_FILE"
}

cleanup_phase() {
  [[ -f "$STATE_FILE" ]] || fail "State not found at $STATE_FILE"
  local admin_token
  admin_token="$(obtain_token "$ADMIN_CLIENT_ID" "$ADMIN_CLIENT_SECRET")"

  local download_link_id version_id game_id language_id tool_id host_image_id
  local tool_type_id platform_id processor_id genre_id
  download_link_id="$(recorded_id downloadLink)"
  version_id="$(recorded_id version)"
  game_id="$(recorded_id game)"
  language_id="$(recorded_id language)"
  tool_id="$(recorded_id tool)"
  host_image_id="$(recorded_id hostImage)"
  tool_type_id="$(recorded_id toolType)"
  platform_id="$(recorded_id platform)"
  processor_id="$(recorded_id processor)"
  genre_id="$(recorded_id genre)"

  if [[ -n "$download_link_id" && -n "$version_id" && -n "$game_id" ]]; then
    delete_resource "delete E2E download link" \
      "/api/v1/games/$game_id/versions/$version_id/download-links/$download_link_id" "$admin_token"
  fi
  if [[ -n "$version_id" && -n "$game_id" ]]; then
    delete_resource "delete E2E game version" "/api/v1/games/$game_id/versions/$version_id" "$admin_token"
  fi
  if [[ -n "$game_id" ]]; then
    delete_resource "delete E2E game" "/api/v1/games/$game_id" "$admin_token"
  fi

  if [[ "$(state_value '.fixture.toolLanguageCreated')" == "true" ]]; then
    remove_tool_language_fixture "$language_id" "$tool_id"
  fi

  [[ -z "$host_image_id" ]] || delete_resource "delete E2E host image" "/api/v1/server-host-images/$host_image_id" "$admin_token"
  [[ -z "$tool_id" ]] || delete_resource "delete E2E programming tool" "/api/v1/programming-tools/$tool_id" "$admin_token"
  [[ -z "$tool_type_id" ]] || delete_resource "delete E2E tool type" "/api/v1/programming-tool-types/$tool_type_id" "$admin_token"
  [[ -z "$language_id" ]] || delete_resource "delete E2E language" "/api/v1/programming-languages/$language_id" "$admin_token"
  [[ -z "$platform_id" ]] || delete_resource "delete E2E platform" "/api/v1/platforms/$platform_id" "$admin_token"
  [[ -z "$processor_id" ]] || delete_resource "delete E2E processor" "/api/v1/processors/$processor_id" "$admin_token"
  [[ -z "$genre_id" ]] || delete_resource "delete E2E genre" "/api/v1/game-genres/$genre_id" "$admin_token"

  rm -f -- "$STATE_FILE"
  log "CLEANUP COMPLETE"
}

usage() {
  printf 'Usage: %s {create|verify|cleanup}\n' "$0" >&2
  exit 64
}

[[ $# -eq 1 ]] || usage
case "$1" in
  create) create_phase ;;
  verify) verify_phase ;;
  cleanup) cleanup_phase ;;
  *) usage ;;
esac
