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

The catalog HTTP boundary uses resource-specific request and response DTOs.
Controllers map those DTOs explicitly and never accept or return JPA entities;
therefore persistence changes do not silently alter the public JSON contract.
The existing endpoint paths, status codes and `id`/`name` response shape remain
compatible.

Paged endpoints return the shared `PageResponse` contract instead of exposing
Spring Data's unstable `PageImpl` serialization. It preserves the established
pagination fields while making their JSON representation application-owned.

## Tooling module

`tooling` owns programming languages, programming tools, tool types and the
language/platform/processor compatibility associations. It depends on catalog
domain types for platforms and processors. The game-version model consumes
tooling domain associations but does not depend on tooling services,
repositories or API controllers.

Moving the association entities together with their composite ID classes keeps
the JPA model inside one feature boundary. Existing endpoints, tables and
foreign-key columns remain unchanged.

The tooling HTTP boundary uses request and response DTOs for programming
languages, tool types and programming tools. A tool refers to its type through
`toolTypeId`; the application service resolves that identifier transactionally
instead of accepting a client-built JPA graph. The `ManyToOne` no longer
cascades writes or deletes into the shared tool type. Paged responses use the
application-owned `PageResponse` contract.

## Shared kernel

`shared` contains only technical abstractions used by more than one feature
module: the mapped base entity and generic repository, service and API support.
It must not depend on `catalog`, `tooling` or future business modules.

The shared API validates each element of bulk request bodies with
`List<@Valid E>`. Feature-specific DTOs, entities, repositories and business
rules must remain in their owning module instead of being promoted to shared.

The shared API also owns the cross-module HTTP error contract. API and Spring
Security failures are represented as RFC Problem Details with stable error
codes; validation details never expose rejected values. Feature-specific
handlers may select a status and code, but must use the same representation.
The complete contract is documented in `docs/api-errors.md`.

Cross-module Bean Validation constraints and their validators live together in
`shared.validation`. Feature-specific validation rules remain inside their
owning module.

## Game module

`game` owns game data, game versions and their download links. Its HTTP API is
rooted at `/api/v1/games`; versions and download links are nested resources so
that their aggregate ownership is explicit. Requests refer to catalog, tooling
and storage resources by identifier, while responses expose module-owned DTOs
instead of JPA entities.

The module consumes catalog domain types for genres and development difficulty,
and tooling domain types for language, platform and processor compatibility.
`DownloadLink` references the public `ServerHostImage` type owned by the storage
domain and does not depend on storage application or infrastructure code.

The application layer resolves every referenced resource transactionally and
checks that language, platform and processor associations all belong to the
selected programming tool. Standard JPA persistence replaces the former native
insert implementation. Database names remain compatible; migration V2 removes
the accidental uniqueness constraint that prevented two games from sharing a
development difficulty.

The HTTP resources are backed by separate application services for games, game
versions and download links. Each service owns its transactions and depends only
on the repositories and collaborating modules required by that resource level.

## Storage module

`storage` owns host-image metadata and the files that back it. Its `api` layer
contains the multipart endpoint and file-specific exception mapping;
`application` contains the host-image service and the file-storage port;
`persistence` owns the JPA repository; and `infrastructure` contains the local
filesystem adapter.

The endpoint path, multipart field names and JPA mappings remain unchanged. The
filesystem adapter uses an externally configured root, generates server-side
filenames, confines every operation to that root, validates size and image
signature, and publishes uploads with an atomic move.

The storage API exposes DTOs rather than `ServerHostImage` entities. Multipart
field names remain `fileHostImage` and `name`. Creation, image replacement and
deletion are coordinated with database transactions: rollback removes newly
written files, while successful commit removes superseded or deleted files.
Clients cannot create metadata or set `imagepath` without using the file
operations.

## Security module

`security` centralizes authentication, route authorization and CORS instead of
placing policy annotations in feature controllers. Public API reads use `GET`
or `HEAD`; all API mutations require the `ADMIN` role. Preflight requests are
evaluated against an external origin allowlist, and all otherwise unmatched
routes are denied.

The current administrator is an in-memory operational identity whose password
is BCrypt-encoded at startup. This boundary can later replace HTTP Basic with an
OIDC/JWT resource server without changing feature modules.
