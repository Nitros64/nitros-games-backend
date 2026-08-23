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
$env:OAUTH2_ISSUER_URI = "http://localhost:8081/realms/nitros-games"
$env:OAUTH2_JWK_SET_URI = "http://localhost:8081/realms/nitros-games/protocol/openid-connect/certs"
$env:OAUTH2_AUDIENCE = "nitros-games-api"
.\mvnw.cmd spring-boot:run
```

Bash:

```shell
export SPRING_PROFILES_ACTIVE=local
export DB_PASSWORD=your-local-password
export OAUTH2_ISSUER_URI=http://localhost:8081/realms/nitros-games
export OAUTH2_JWK_SET_URI=http://localhost:8081/realms/nitros-games/protocol/openid-connect/certs
export OAUTH2_AUDIENCE=nitros-games-api
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
- `APP_SECURITY_ALLOWED_ORIGINS`
- `OAUTH2_ISSUER_URI`
- `OAUTH2_JWK_SET_URI`
- `OAUTH2_AUDIENCE`

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
require a Bearer JWT whose `realm_access.roles` contains `ADMIN`. The resource
server also preserves standard `SCOPE_` authorities and uses
`preferred_username` as the authenticated principal when available.

`OAUTH2_ISSUER_URI`, `OAUTH2_JWK_SET_URI` and `OAUTH2_AUDIENCE` are mandatory in
production. Spring Security verifies the signature, issuer, time constraints
and audience. Keeping the externally visible issuer separate from the internal
JWK URL allows the Compose API to validate tokens issued as
`http://localhost:8081` while resolving keys through the Docker network.

Clients send `Authorization: Bearer <token>` on every protected request. The
application creates no authentication session or cookie and disallows
credentialed CORS, so CSRF is disabled for this stateless header-only contract.
Use HTTPS for the API and identity provider in production.

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

Tests activate `test` themselves, use an in-memory H2 database and inject mock
JWTs into MVC requests. They do not read database or identity-provider secrets.
