# Build baseline

This baseline makes the project build and run its tests without requiring the
developer's local MySQL instance.

## Verification

Requirements:

- JDK 21
- Network access to Maven Central on the first run

Run the complete baseline with:

```shell
./mvnw clean verify
```

On Windows:

```powershell
.\mvnw.cmd clean verify
```

## Test isolation

Tests activate the `test` profile explicitly and use an in-memory H2 database
in MySQL compatibility mode. Hibernate creates the schema when the test context
starts and drops it when the context closes. The test profile never uses the
runtime database configuration. It is fully self-contained under
`src/test/resources/application-test.properties`.

The baseline currently characterizes:

- application context and JPA metamodel startup;
- the number of mapped entities;
- the empty game genre catalogue response;
- the existing not-found HTTP response contract;
- the existing `NoNumberValidator` behavior.

## Known limitations

- H2 compatibility mode does not prove that the migration is compatible with
  MySQL; use the `mysql-it` Maven profile for that verification.
- H2 is deliberately limited to the test classpath and cannot be selected in a
  runtime environment.
- Security, API redesign and domain refactoring are deliberately outside this
  baseline.
- The MySQL integration test is intentionally opt-in because it requires a
  working Docker environment.

The persistence phase now validates versioned Flyway migrations against MySQL
8.4.11 with Testcontainers. See `docs/database-migrations.md`.
