# Runtime configuration

The application has explicit `local`, `test` and `prod` profiles. No runtime
profile is selected by default, so application startup fails instead of
silently connecting with embedded credentials.

## Local development

Set the active profile and database password before starting the application.

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_PASSWORD = "your-local-password"
$env:APP_SECURITY_ADMIN_PASSWORD = "a-long-random-local-password"
.\mvnw.cmd spring-boot:run
```

Bash:

```shell
export SPRING_PROFILES_ACTIVE=local
export DB_PASSWORD=your-local-password
export APP_SECURITY_ADMIN_PASSWORD=a-long-random-local-password
./mvnw spring-boot:run
```

`DB_URL` and `DB_USERNAME` have local defaults. `DB_PASSWORD` intentionally
does not. `.env.example` is a reference for IDEs or tools that support dotenv
files; Spring Boot does not load `.env` files by itself.

The local profile keeps SQL logging and lazy loading outside transactions for
development compatibility. Flyway owns schema changes and Hibernate only
validates the mappings. Existing local schemas can be adopted at version 1
with `FLYWAY_BASELINE_ON_MIGRATE`; this must not be enabled automatically in
production.

## Production

Activate `prod` and provide all required database settings through the runtime
environment or a secrets manager:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_STORAGE_HOST_IMAGES_DIRECTORY`
- `APP_SECURITY_ADMIN_USERNAME`
- `APP_SECURITY_ADMIN_PASSWORD`
- `APP_SECURITY_ALLOWED_ORIGINS`

`DB_URL` must use the TLS settings required by the production MySQL provider.
Production validates the existing schema and never creates or updates it.

Optional pool settings are `DB_POOL_MIN_IDLE`, `DB_POOL_MAX_SIZE`,
`DB_CONNECTION_TIMEOUT_MS` and `DB_VALIDATION_TIMEOUT_MS`.
`APP_LOG_LEVEL` controls the production root log level and defaults to `INFO`.

The container image and single-host Compose deployment are documented in
`docs/docker.md`.

## Health probes

The Actuator health endpoints are public. `/actuator/health/liveness`
reports whether the application process should be restarted, while
`/actuator/health/readiness` also checks the database and reports whether the
instance should receive traffic. Component details are never returned.

`/actuator/prometheus` exposes JVM, HTTP, database-pool and process metrics in
Prometheus format. It requires the same administrator authentication used for
API mutations and must only be scraped over HTTPS in production. Request
latency histograms are enabled in the production profile.

Production logs are emitted as one Logstash-compatible JSON object per line.
Every HTTP response includes `X-Request-ID`; a safe identifier supplied by the
client is preserved, otherwise the application generates a UUID. The same
value is stored as `requestId` in the logging context, making it possible to
trace all logs produced while handling a request. Client-provided identifiers
are restricted to 64 letters, digits, dots, underscores or hyphens.

## Host-image storage

Production requires `APP_STORAGE_HOST_IMAGES_DIRECTORY` to point to a writable,
persistent directory mounted outside the application image. Local development
defaults to `uploadImageFileHost` under the working directory.

Uploads are limited by `APP_STORAGE_MAX_FILE_SIZE` (default `10MB`). The complete
multipart request is limited independently by `APP_STORAGE_MAX_REQUEST_SIZE`
(default `11MB`, allowing for multipart overhead). PNG, JPEG and GIF are the only
accepted formats; the adapter verifies both the declared content type and the
file signature.

## HTTP security

All `GET /api/**` endpoints remain public. `POST`, `PUT` and `DELETE` requests
require an administrator authenticated with HTTP Basic and the `ADMIN` role.
The application is stateless and does not create login sessions.

Configure the account with `APP_SECURITY_ADMIN_USERNAME` and a random
`APP_SECURITY_ADMIN_PASSWORD` of at least 16 characters. Production requires
both values. HTTP Basic must only be exposed through HTTPS; it is an incremental
operator-access mechanism, not a replacement for a future user domain or an
OIDC/JWT identity provider.

Clients must send the `Authorization` header explicitly on each mutation. The
server does not emit a browser Basic-authentication challenge, does not use
authentication cookies and does not allow credentialed CORS requests. CSRF is
therefore disabled for this stateless header-only API contract.

`APP_SECURITY_ALLOWED_ORIGINS` is a comma-separated allowlist of complete web
origins, for example `https://admin.example.com,https://www.example.com`.
Wildcards are rejected. Local development defaults to `http://localhost:4200`;
production requires an explicit allowlist.

## Credential rotation

The previous MySQL password was committed and pushed. Removing it from the
latest revision does not remove it from Git history. Rotate that database
credential immediately and revoke the old value. Rewriting published history
is a separate repository-administration operation and requires coordination
with every clone and collaborator.

## Tests

Tests activate `test` themselves and use an in-memory H2 database. They do not
read `DB_URL`, `DB_USERNAME` or `DB_PASSWORD`.
