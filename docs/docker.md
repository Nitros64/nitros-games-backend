# Container deployment

The repository includes a production-oriented multi-stage `Dockerfile` and a
`compose.yaml` stack for the API and MySQL 8.4. The runtime image contains only
the Java runtime and the packaged application. It runs as UID `10001`, uses a
read-only root filesystem in Compose and writes host images only to a dedicated
persistent volume.

## Local container stack

Copy `.env.example` to `.env` and replace every placeholder password. The
database password, root password and administrator password must be different,
random values. `.env` is ignored by Git.

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose config
docker compose build
docker compose up -d --wait
docker compose ps
```

Bash:

```shell
cp .env.example .env
docker compose config
docker compose build
docker compose up -d --wait
docker compose ps
```

The API is available at `http://localhost:8080` by default. Change `APP_PORT`
to publish a different host port. MySQL is intentionally not published to the
host.

Verify readiness:

```shell
curl --fail http://localhost:8080/actuator/health/readiness
```

The readiness group includes the database connection. The liveness group does
not include MySQL, preventing an external database outage from causing an
application restart loop. Health details are never exposed.

Inspect logs and stop the stack without deleting data:

```shell
docker compose logs -f api
docker compose down
```

`docker compose down --volumes` permanently deletes the MySQL and host-image
volumes and should only be used when that data is intentionally disposable.

## Persistence and startup

- `mysql-data` owns the MySQL data directory.
- `host-images` owns `/var/lib/nitros-games/host-images`.
- The API waits for the MySQL health check before starting.
- Flyway applies pending migrations before Hibernate validates the schema.
- Both services use bounded JSON log rotation.
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

The platform must preserve the host-image path, terminate HTTPS before the API,
and probe `/actuator/health/liveness` and `/actuator/health/readiness`. Do not
publish the MySQL port or expose any Actuator endpoint other than health.
