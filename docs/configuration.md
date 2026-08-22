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
.\mvnw.cmd spring-boot:run
```

Bash:

```shell
export SPRING_PROFILES_ACTIVE=local
export DB_PASSWORD=your-local-password
./mvnw spring-boot:run
```

`DB_URL` and `DB_USERNAME` have local defaults. `DB_PASSWORD` intentionally
does not. `.env.example` is a reference for IDEs or tools that support dotenv
files; Spring Boot does not load `.env` files by itself.

The local profile preserves the current development behavior, including
Hibernate schema updates, SQL logging and lazy loading outside transactions.
Those settings must not be used in production.

## Production

Activate `prod` and provide all required database settings through the runtime
environment or a secrets manager:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

`DB_URL` must use the TLS settings required by the production MySQL provider.
Production validates the existing schema and never creates or updates it.

Optional pool settings are `DB_POOL_MIN_IDLE`, `DB_POOL_MAX_SIZE`,
`DB_CONNECTION_TIMEOUT_MS` and `DB_VALIDATION_TIMEOUT_MS`.

## Credential rotation

The previous MySQL password was committed and pushed. Removing it from the
latest revision does not remove it from Git history. Rotate that database
credential immediately and revoke the old value. Rewriting published history
is a separate repository-administration operation and requires coordination
with every clone and collaborator.

## Tests

Tests activate `test` themselves and use an in-memory H2 database. They do not
read `DB_URL`, `DB_USERNAME` or `DB_PASSWORD`.
