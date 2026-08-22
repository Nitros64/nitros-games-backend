# Modular architecture

The application is being migrated incrementally from package-by-layer to
package-by-feature. Each top-level feature package is intended to become an
independently understandable module with these internal layers:

- `domain`: entities and domain rules;
- `application`: use cases and transaction orchestration;
- `persistence`: repository adapters;
- `web`: HTTP adapters.

## Catalog module

`catalog` is the first vertical module. It owns development difficulties, game
genres, platforms and processors. Existing endpoint paths, database table names
and JSON contracts remain unchanged.

Games and programming tools currently reference catalog domain entities through
JPA relationships. They must not depend on catalog services, repositories or
controllers. Generic base controllers, services, repositories and entities are
still in legacy layer packages; they will move to a shared kernel after another
vertical module has been extracted and their actual common surface is clear.

The architecture test records the current boundary and prevents catalog types
from being moved back into layer-oriented packages.
