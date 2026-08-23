# Database migrations

Flyway owns the MySQL schema. Hibernate validates the mappings and must not
create or alter tables in local or production environments.

## New databases

At application startup, Flyway applies the scripts in
`src/main/resources/db/migration/mysql` before Hibernate validates the model.
The initial migration creates the 15 tables represented by the current JPA
model.

## Existing databases

The local profile enables `baseline-on-migrate` at version 1 so an existing
development schema created by Hibernate can be adopted without replaying the
initial migration. This convenience is restricted to local development.

Production does not baseline automatically. Before deploying this change over
an existing production schema, compare that schema with V1, back it up and
explicitly baseline it at version 1. Automatic baselining could legitimize an
unknown or incomplete production schema.

## Verification

Fast tests remain self-contained with H2:

```powershell
.\mvnw.cmd clean verify
```

The MySQL integration profile requires Docker. It creates a disposable MySQL
8.4.11 database, executes Flyway, validates all Hibernate mappings and runs a
set of repository queries, including detailed, lightweight and hierarchical
game-resource loading:

```powershell
.\mvnw.cmd clean verify -Pmysql-it
```

The container and database are removed after the integration test.
