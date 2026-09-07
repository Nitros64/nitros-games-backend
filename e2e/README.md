# Local end-to-end release rehearsal

This rehearsal exercises the real local Keycloak, API, MySQL and image-storage
containers. It is intentionally independent from CI/CD and staging.

It has three explicit phases:

```text
create -> verify -> restart -> verify -> restart -> verify -> cleanup
```

The script gives every resource a unique `e2e-...` run identifier and stores
the exact database identifiers in a local state file. By default that file is:

```text
${TMPDIR:-/tmp}/nitros-games-release-rehearsal-state.json
```

Set `E2E_STATE_FILE` before every phase to choose another location. Do not edit
the state file between phases.

## Prerequisites

- Docker with Compose
- Bash
- `curl`, `jq`, `base64` and `awk`
- a local `.env` based on `.env.example`

Use different random values for these two development-only clients:

```dotenv
NITROS_GAMES_CLI_SECRET=<local-admin-client-secret>
NITROS_GAMES_READER_CLI_SECRET=<different-local-reader-client-secret>
```

No real secret is stored in the repository. The reader client receives a valid
JWT with the API audience but has no `ADMIN` realm role; it exists solely to
prove the `403 Forbidden` behavior.

After changing the imported realm, recreate Keycloak so it imports the new
reader client, then start the complete stack:

```shell
docker compose --profile identity up -d --force-recreate keycloak
docker compose --profile identity up -d --build --wait
docker compose ps
```

## Rehearsal

Create the isolated data and execute the HTTP scenarios:

```shell
./e2e/release-rehearsal.sh create
```

Verify the exact recorded resources without changing them:

```shell
./e2e/release-rehearsal.sh verify
```

Restart only the API, wait for readiness, and verify again:

```shell
docker compose restart api
docker compose up -d --wait api
./e2e/release-rehearsal.sh verify
```

Restart MySQL, wait for the database and API to recover, and verify again:

```shell
docker compose restart mysql
docker compose up -d --wait mysql api
./e2e/release-rehearsal.sh verify
```

Finally remove only the resources whose IDs were recorded by this rehearsal:

```shell
./e2e/release-rehearsal.sh cleanup
```

The script deliberately does not restart containers. This keeps persistence
checks visible and under operator control.

## Staging through SSM

The staging CD bundle installs this same script at
`/opt/nitros-games/e2e/release-rehearsal.sh`; CD does not execute it. Run each
phase manually in an `AWS-RunShellScript` command from `/opt/nitros-games` with:

```bash
export E2E_API_BASE_URL=http://127.0.0.1:8080
export E2E_TOKEN_URL=http://127.0.0.1:8081/realms/nitros-games/protocol/openid-connect/token
export E2E_STATE_FILE=/opt/nitros-games/e2e-state/<unique-run-id>.json
./e2e/release-rehearsal.sh create
```

Use the same variables and state path for `verify` and `cleanup`. The script
reads the two client secrets from the root-owned `/opt/nitros-games/.env`; do
not include secret values in the SSM command.

The full staging persistence sequence is:

1. `create`, then `verify`.
2. Restart only `api`, then `verify`.
3. Restart only `mysql`, then `verify`.
4. Restart only `keycloak`, then `verify`.
5. Deploy a known-good immutable application SHA, then `verify`.
6. `cleanup`.

## Controlled `tool_lang` fixture

The application has no write endpoint for language/tool compatibility, while a
game version requires that relationship. After creating both records through
the API, the script inserts exactly one `tool_lang` row using their recorded
IDs. Cleanup deletes that exact pair after deleting the game version that uses
it. No other database rows are selected or removed by SQL.

## Coverage

The `create` phase checks:

- public readiness;
- `401` for a mutation without a token;
- `403` for a valid non-administrator token;
- a valid administrator token;
- catalog, tooling, host-image, game, version and download-link creation;
- public read-back and combined game search with bounded pagination;
- representative `400`, `404` and `409` responses.

The `verify` phase checks every recorded API resource, the exact `tool_lang`
fixture and the uploaded image bytes in the Docker volume. There is no public
endpoint for downloading image bytes, so that last assertion uses a read-only
`test -f` inside the API container.

The harness configures no identity provider itself. Local Compose or staging
must already provide the documented clients and secrets.
