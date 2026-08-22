# Modular architecture

The application is being migrated incrementally from package-by-layer to
package-by-feature. Each top-level feature package is intended to become an
independently understandable module with these internal layers:

- `domain`: entities and domain rules;
- `application`: use cases and transaction orchestration;
- `persistence`: repository adapters;
- `api`: public HTTP entry points and their transport contracts.

## Catalog module

`catalog` is the first vertical module. It owns development difficulties, game
genres, platforms and processors. Existing endpoint paths, database table names
and JSON contracts remain unchanged.

Games and programming tools currently reference catalog domain entities through
JPA relationships. They must not depend on catalog services, repositories or
API controllers. Generic base controllers, services, repositories and entities are
still in legacy layer packages; they will move to a shared kernel after another
vertical module has been extracted and their actual common surface is clear.

The architecture test records the current boundary and prevents catalog types
from being moved back into layer-oriented packages.

## Tooling module

`tooling` owns programming languages, programming tools, tool types and the
language/platform/processor compatibility associations. It depends on catalog
domain types for platforms and processors. The game-version model consumes
tooling domain associations but does not depend on tooling services,
repositories or API controllers.

Moving the association entities together with their composite ID classes keeps
the JPA model inside one feature boundary. Existing endpoints, tables and
foreign-key columns remain unchanged.

## Shared kernel

`shared` contains only technical abstractions used by more than one feature
module: the mapped base entity and generic repository, service and API support.
It must not depend on `catalog`, `tooling` or future business modules.

The shared API validates each element of bulk request bodies with
`List<@Valid E>`. Feature-specific DTOs, entities, repositories and business
rules must remain in their owning module instead of being promoted to shared.

## Game module

`game` owns game data, game versions and their download links. Its internal
layers currently comprise `domain`, `application` and `persistence`; an `api`
package will be introduced only when the module exposes HTTP use cases.

The module consumes catalog domain types for genres and development difficulty,
and tooling domain types for language, platform and processor compatibility.
`DownloadLink` temporarily references the legacy `ServerHostImage` entity. That
dependency is recorded explicitly and will be replaced by a stable storage
module boundary when host images and file handling are extracted.

This migration changes Java package ownership only. JPA table names, columns,
relationships and native SQL remain unchanged.
