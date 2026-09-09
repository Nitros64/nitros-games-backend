# Container deployment

The repository includes a production-oriented multi-stage `Dockerfile` and a
`compose.yaml` stack for the API and MySQL 8.4, plus an optional Keycloak
development profile. The runtime uses the digest-pinned Eclipse Temurin 21 JRE
Alpine image. It contains the packaged application and the single Amazon RDS
`eu-west-1` RSA2048 G1 root required by production. It runs as UID `10001`, uses
a read-only root filesystem in Compose and writes host images only to a
dedicated persistent volume.

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
inside the Docker network. When using production RDS, `DB_URL` must contain its
real DNS endpoint and exactly one `sslMode=VERIFY_IDENTITY`; application startup
rejects weaker modes or a truststore override. Credentials are injected through
the platform's secret manager rather than committed or baked into the image.

During the image build, the official `eu-west-1` RDS root bundle is verified
against a pinned SHA-256. The build extracts the first certificate and verifies
its AWS-published SHA-1 thumbprint before importing only
`rds-ca-rsa2048-g1` into the JVM truststore. Certificate download never occurs
at application startup. When AWS changes the official bundle, update the pinned
checksum only after reviewing its roots and the CA assigned to RDS.

The bundled Keycloak uses `start-dev`; it is a local/staging identity provider,
not a production topology. Production uses Cognito and supplies the issuer, JWK
Set, resource ID, access/admin scopes and explicit client-ID allowlist described
in `docs/configuration.md`.

The platform must preserve the host-image path, terminate HTTPS before the API,
and probe `/actuator/health/liveness` and `/actuator/health/readiness`. Do not
publish the MySQL port. Restrict `/actuator/prometheus` to the monitoring system
and transmit Bearer tokens only over HTTPS.
