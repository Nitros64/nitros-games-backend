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

- H2 compatibility mode does not prove that Hibernate's generated schema is
  fully compatible with MySQL.
- There are no migrations yet, so production schema drift is not checked.
- H2 is deliberately limited to the test classpath and cannot be selected in a
  runtime environment.
- Security, API redesign and domain refactoring are deliberately outside this
  baseline.
- Compilation still reports unchecked operations in `CustomExceptionMessage`.

A later persistence phase should add Testcontainers with the supported MySQL
version and validate versioned database migrations against it.
