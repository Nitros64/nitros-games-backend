# Container deployment

The repository includes a production-oriented multi-stage `Dockerfile` and a
`compose.yaml` stack for the API and MySQL 8.4, plus an optional Keycloak
development profile. The runtime image contains only the Java runtime and the
packaged application. It runs as UID `10001`, uses a read-only root filesystem
in Compose and writes host images only to a dedicated persistent volume.

## Local container stack

Copy `.env.example` to `.env` and replace every placeholder. Database
passwords, the Keycloak bootstrap password and the CLI client secret must be
different random values. `.env` is ignored by Git.

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose --profile identity config
docker compose build
docker compose --profile identity up -d --wait
docker compose ps
```

Bash:

```shell
cp .env.example .env
docker compose --profile identity config
docker compose build
docker compose --profile identity up -d --wait
docker compose ps
```

The API is available at `http://localhost:8080` and Keycloak at
`http://localhost:8081` by default. Change `APP_PORT` or `KEYCLOAK_PORT` to use
different host ports. MySQL is intentionally not published to the host.

Verify readiness:

```shell
curl --fail http://localhost:8080/actuator/health/readiness
```

Obtain an operational token and verify the protected Prometheus endpoint:

```powershell
$env:NITROS_GAMES_CLI_SECRET = "the-same-value-configured-in-.env"

$token = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/realms/nitros-games/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "client_credentials"
    client_id = "nitros-games-cli"
    client_secret = $env:NITROS_GAMES_CLI_SECRET
  }

Invoke-WebRequest http://localhost:8080/actuator/prometheus `
  -Headers @{ Authorization = "Bearer $($token.access_token)" }
```

The readiness group includes the database connection. The liveness group does
not include MySQL, preventing an external database outage from causing an
application restart loop. Health details are never exposed.

The staging deployment records the currently active immutable image before
switching versions. If the candidate does not pass readiness, the script
restores the previous image, waits for it to become healthy and reports the
rollback in the GitHub Actions summary. The workflow still fails so the rejected
release remains visible. Database migrations must remain backward compatible:
rolling back the application image does not reverse Flyway migrations.

Inspect logs and stop the stack without deleting data:

```shell
docker compose logs -f api
docker compose --profile identity down
```

`docker compose down --volumes` permanently deletes the MySQL and host-image
volumes and should only be used when that data is intentionally disposable.

## Persistence and startup

- `mysql-data` owns the MySQL data directory.
- `host-images` owns `/var/lib/nitros-games/host-images`.
- The API waits for the MySQL health check before starting.
- Flyway applies pending migrations before Hibernate validates the schema.
- Both services use bounded log rotation; the API emits structured JSON logs.
- Compose applies memory and process-count limits; the JVM derives its heap from
  the container memory limit.
- The API receives `SIGTERM` through a minimal init process and has a 30-second
  shutdown grace period.

## External production platform

`compose.yaml` is suitable for a single-host deployment and local production
validation. The internal MySQL connection disables TLS because traffic remains
inside the Docker network. When using a managed or remote database, provide a
TLS-enabled `DB_URL` and inject credentials through the platform's secret
manager rather than an environment file.

The bundled Keycloak uses `start-dev` and ephemeral storage; it is a local/demo
identity provider, not a production topology. Production should supply a
managed OIDC provider or a hardened, persistent Keycloak deployment and set
`OAUTH2_ISSUER_URI`, `OAUTH2_JWK_SET_URI` and `OAUTH2_AUDIENCE`.

The platform must preserve the host-image path, terminate HTTPS before the API,
and probe `/actuator/health/liveness` and `/actuator/health/readiness`. Do not
publish the MySQL port. Restrict `/actuator/prometheus` to the monitoring system
and transmit Bearer tokens only over HTTPS.
