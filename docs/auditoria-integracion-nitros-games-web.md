# Auditoría para integrar Nitros Games Web

> Este documento conserva la auditoría presentada en la conversación. Su creación no implica una nueva revisión del código ni una comprobación del estado actual del backend.

**Resultado:** existe el CRUD básico de juegos y su autorización administrativa. Faltan el ciclo de publicación, la separación de lecturas públicas/administrativas y el modelo de portadas y capturas.

Inspección estática limitada a archivos relevantes de `src/main`. Durante la auditoría no se modificaron archivos ni se ejecutaron aplicación, pruebas o migraciones. **IMPLEMENTED** significa presente en el código, no verificado en ejecución.

## 1. Funcionalidad existente y clasificación

| Funcionalidad | Estado | Evidencia |
|---|---|---|
| Crear un juego | **IMPLEMENTED** | `POST`, DTO validado, resolución de géneros y persistencia transaccional. |
| Editar información actual | **IMPLEMENTED** | `PUT` de nombre, descripción, jam, número de desarrolladores y géneros. |
| Eliminar un juego | **IMPLEMENTED** | Borrado físico; el esquema define cascadas para versiones, enlaces y asociaciones de géneros. |
| Subir y gestionar portada/capturas | **MISSING** | No existen relaciones, entidades ni endpoints de imágenes de juegos. |
| Base de almacenamiento de imágenes | **PARTIAL** | Existen adaptadores filesystem/S3, pero dedicados a imágenes de hosts de descarga. |
| Publicar | **MISSING** | Sin estado, transición ni validaciones de publicación. |
| Despublicar conservando datos | **MISSING** | No existe operación equivalente. |
| Listar exclusivamente publicados | **MISSING** | Los listados actuales recuperan todos los juegos. |
| Administradores consultando borradores | **MISSING** | No existen borradores ni consultas administrativas diferenciadas. |
| Estado de desarrollo independiente | **MISSING** | No hay `developmentStatus`. |
| Autenticación y rol administrativo | **IMPLEMENTED** | JWT, conversión de autoridades y restricciones de escritura. |
| Paginación y errores estructurados | **IMPLEMENTED** | `PageResponse`, límites configurados y Problem Details. |
| Configuración efectiva del entorno y esquema aplicado | **UNKNOWN** | No se inspeccionaron valores externos ni una base de datos en ejecución. |

### Archivos principales inspeccionados

- API: [GameController.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/api/GameController.java).
- Contratos: [GameRequest.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/api/dto/GameRequest.java), [GameResponse.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/api/dto/GameResponse.java).
- Modelo: [GameData.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/domain/GameData.java).
- Casos de uso: [GameApplicationService.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/application/GameApplicationService.java).
- Consultas: [GameDataRepository.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/persistence/GameDataRepository.java).
- Seguridad: [SecurityConfiguration.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/security/SecurityConfiguration.java).
- Imágenes existentes: [ServerHostImageController.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/storage/api/ServerHostImageController.java).

## 2. Endpoints y contratos actuales

### Juegos

Base: `/api/v1/games`.

| Método y ruta | Acceso | Resultado |
|---|---|---|
| `GET /api/v1/games` | Público | Array completo, sin paginación. |
| `GET /api/v1/games/paged` | Público | Página de juegos. |
| `GET /api/v1/games/search` | Público | Página filtrada por `name`, `genreId`, `jam`. |
| `GET /api/v1/games/{gameId}` | Público | Juego individual o `404`. |
| `POST /api/v1/games` | ADMIN | `201`, cuerpo y `Location`. |
| `PUT /api/v1/games/{gameId}` | ADMIN | `200`, cuerpo actualizado. |
| `DELETE /api/v1/games/{gameId}` | ADMIN | `204`; inexistente devuelve `404`. |

**JSON actual de creación y edición:**

```json
{
  "name": "Juego ejemplo",
  "description": "Descripción breve",
  "jam": false,
  "developerCount": 1,
  "genreIds": [1, 2]
}
```

**Respuesta:** los mismos campos, más `id`.

Validaciones actuales:

- `name`: obligatorio, entre 4 y 30 caracteres.
- `description`: obligatoria, **entre 4 y 30 caracteres**.
- `developerCount`: mayor que cero.
- `genreIds`: no vacío; identificadores positivos y no nulos.
- Todos los géneros deben existir; en caso contrario se devuelve `404`.
- `jam` es un `boolean` primitivo: el contrato no exige su presencia mediante `@NotNull`.
- `PUT` recibe el contrato completo; no existe edición parcial con `PATCH`.

No incluye imágenes, estados, slug ni fechas editoriales.

### Versiones y enlaces

Controladores: [GameVersionController.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/api/GameVersionController.java) y [DownloadLinkController.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/game/api/DownloadLinkController.java).

Ambos ofrecen:

- `GET` colección e individual.
- `POST` colección.
- `PUT` y `DELETE` individual.
- Lecturas públicas y escrituras ADMIN.

**Versiones:** `/api/v1/games/{gameId}/versions`

```json
{
  "name": "Versión inicial",
  "programmingLanguageId": 1,
  "programmingToolId": 2,
  "platformId": 3,
  "processorId": 4
}
```

La respuesta añade `id` y `gameId`.

**Enlaces:** `/api/v1/games/{gameId}/versions/{versionId}/download-links`

```json
{
  "link": "https://example.com/download",
  "serverHostImageId": 1
}
```

La respuesta añade `id` y `gameVersionId`.

Comportamiento relevante:

- Se comprueba la pertenencia versión→juego y enlace→versión→juego.
- Se valida la asociación lenguaje/herramienta.
- Plataforma y procesador deben existir; **no se exige que estén asociados a la herramienta**.
- `link` tiene longitud 4–100 y unicidad en BD, pero el DTO **no valida formato URL ni esquema HTTP/HTTPS**.
- Ninguna consulta comprueba publicación.

### Imágenes existentes

Base canónica: `/api/v1/server-host-images`. También existe el alias `/api/v1/serverhostimage`.

| Operación | Contrato |
|---|---|
| `GET` colección, `/paged`, `/search?name=`, `/{id}` | Devuelve metadatos. |
| `POST` base o `/upload_image` | Multipart: `name`, `fileHostImage`. |
| `PUT /{id}/image` | Multipart: `name`, `fileHostImage`. |
| `PUT /{id}/name` | Multipart: `name`. |
| `DELETE /{id}` | Elimina metadatos y programa limpieza del archivo. |

Respuesta:

```json
{
  "id": 1,
  "name": "MediaHost",
  "imagepath": "identificador-del-archivo.png"
}
```

**`imagepath` es una clave de almacenamiento, no una URL de visualización garantizada.**

Estas imágenes están referenciadas por `DownloadLink.serverImage`. No representan portada ni capturas de un juego.

### Paginación y errores

[PageResponse.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/shared/api/PageResponse.java) define:

```text
content, pageable, last, totalPages, totalElements,
size, number, sort, first, numberOfElements, empty
```

`pageable` contiene `pageNumber`, `pageSize`, `sort`, `offset`, `paged`, `unpaged`. `sort` contiene `empty`, `sorted`, `unsorted`.

Parámetros: `page`, `size`, `sort`; tamaño predeterminado 20 y máximo 100. El límite no afecta al listado completo sin paginación.

[RestExceptionHandler.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/shared/api/error/RestExceptionHandler.java) devuelve `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more request fields are invalid",
  "instance": "/api/v1/games",
  "code": "validation_failed",
  "errors": [
    {
      "field": "name",
      "code": "Size",
      "message": "..."
    }
  ]
}
```

Otros códigos existentes: `resource_not_found`, `data_conflict`, `malformed_json`, `authentication_required`, `access_denied`, `image_storage_error`.

## 3. Carencias y condicionantes de integración

### Publicación — MISSING

No existe `DRAFT/PUBLISHED` en entidad, DTO, servicio, repositorio o migraciones inspeccionadas.

**Actualmente, crear un juego lo hace inmediatamente recuperable por los endpoints públicos.**

También falta el estado de desarrollo. Deben modelarse independientemente:

```text
publicationStatus: DRAFT | PUBLISHED
developmentStatus: IN_DEVELOPMENT | COMING_SOON | ...
```

Los valores completos de `developmentStatus` son una decisión pendiente de producto. Un juego `IN_DEVELOPMENT` puede estar publicado.

### Portada y capturas — MISSING; almacenamiento reutilizable — PARTIAL

[HostImageStorage.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/storage/application/HostImageStorage.java) solo declara `store` y `delete`.

- Filesystem incluye un método concreto `load`, pero no forma parte de la interfaz.
- El adaptador S3 inspeccionado implementa subida y borrado.
- No existe entrega de binarios ni generación de URL en los controladores de imágenes inspeccionados.
- Faltan propiedad por juego, portada única, capturas ordenadas y metadatos de presentación.

El validador permite PNG, JPEG y GIF mediante MIME y prefijo binario. No admite WebP/AVIF y no comprueba dimensiones ni decodificación completa.

### Autenticación — IMPLEMENTED; acceso editorial — MISSING

El backend valida Bearer JWT y normaliza roles de Keycloak, grupos de Cognito y scope administrativo.

**Problema para las nuevas rutas:** la regla actual permite cualquier `GET/HEAD /api/**`. Añadir `/api/v1/admin/games` sin cambiar el orden de reglas lo dejaría público.

No se identifica un mecanismo de login propio: esta API valida tokens. La disponibilidad y configuración del emisor externo son **UNKNOWN** en este alcance.

### Perfiles — IMPLEMENTED; adaptación local a Next.js — PARTIAL

| Perfil | Configuración inspeccionada |
|---|---|
| Común | Hibernate `validate`; OSIV desactivado; Flyway; paginación 20/100; multipart 10 MB por archivo y 11 MB por petición. |
| `local` | MySQL local; filesystem; CORS predeterminado `http://localhost:4200`; emisor local configurable. **Reactiva OSIV y lazy loading fuera de transacción.** Baseline Flyway habilitado por defecto. |
| `staging` | Incluye `runtime` mediante grupo. No hay archivo específico de staging entre los recursos inspeccionados. |
| `prod` | Incluye `runtime`; activa comprobación de JDBC con `VERIFY_IDENTITY` y hostname RDS. |
| `runtime` | BD, CORS e identidad desde variables; almacenamiento predeterminado filesystem, seleccionable mediante configuración. |

Fuentes: [application-local.properties](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/resources/application-local.properties), [application.properties](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/resources/application.properties), [application-runtime.properties](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/resources/application-runtime.properties), [ProductionJdbcTlsPolicy.java](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/java/com/nitros64/nitros_games_backend/configuration/ProductionJdbcTlsPolicy.java).

Para llamadas directas del navegador, debe configurarse el origen real de Next.js. CORS actualmente permite `GET/HEAD/POST/PUT/DELETE/OPTIONS`, **no `PATCH`**, y no permite credenciales por cookies.

## 4. Cambios necesarios, clase por clase

Los siguientes son **cambios propuestos**, no funcionalidad existente.

| Clase existente o nueva | Cambio |
|---|---|
| `GameData` | Añadir `publicationStatus` y `developmentStatus` independientes. Métodos explícitos para publicar/despublicar. Nuevos juegos en `DRAFT`. |
| Nuevos `PublicationStatus`, `DevelopmentStatus` | Enums persistidos como texto; cerrar vocabulario de desarrollo antes de implementar. |
| `GameRequest`, `SaveGameCommand` | Añadir estado de desarrollo; ampliar descripción coordinadamente con BD. No permitir publicación accidental desde edición general. |
| `GameDetails`, `GameResponse`, `GameApiMapper` | Propagar estados y referencias de portada/capturas. |
| `GameSearchCriteria` | Filtros administrativos por publicación/desarrollo. |
| `GameDataRepository` | Consultas públicas restringidas a `PUBLISHED`, incluyendo conteo, búsqueda y detalle. Consultas administrativas separadas. |
| `GameApplicationService` | Crear borradores, publicar/despublicar, separar lecturas públicas/administrativas y coordinar borrado de medios. |
| `GameController` | Mantener lecturas públicas filtradas y contratos existentes donde sea compatible. |
| Nuevo `AdminGameController` | Listar/ver todos los estados; operaciones editoriales protegidas. |
| `GameVersionApplicationService`, `DownloadLinkApplicationService` | Comprobar visibilidad del juego en lecturas públicas, incluidas listas vacías. |
| `GameVersionRepository`, `DownloadLinkRepository` | Incorporar predicados de publicación o apoyar una comprobación central del padre. |
| `GameVersionController`, `DownloadLinkController` | Separar acceso público de acceso administrativo a versiones/enlaces de borradores. |
| Nuevos `GameMedia`, `GameMediaRepository` | Asociación al juego, tipo `COVER/SCREENSHOT`, clave, MIME, texto alternativo y orden. |
| Nuevos `GameMediaService`, `GameMediaController`, DTOs | Subida, sustitución de portada, eliminación, metadatos, orden y lectura autorizada. |
| `HostImageStorage`, adaptadores y configuración | Extraer almacenamiento reutilizable o añadir uno específico para juegos; incluir lectura y separación de claves. Preservar imágenes de hosts existentes. |
| `HostImageStorageHandler` o equivalente nuevo | Reutilizar compensaciones transaccionales y contemplar limpieza fallida. |
| `SecurityConfiguration` | Proteger `/api/v1/admin/**` **antes** de permitir lecturas públicas. |
| `RestExceptionHandler` | Añadir errores editoriales estables, por ejemplo `game_not_publishable`; conflictos de concurrencia si se implementan. |

**No reutilizar `ServerHostImage` como entidad de portada/captura:** tiene una función distinta y relaciones existentes con descargas.

## 5. Contratos propuestos para Next.js

### Separación de lectura y edición

| Endpoint propuesto | Comportamiento |
|---|---|
| `GET /api/v1/games/paged` y `/search` | Solo publicados; conservar `PageResponse`. |
| `GET /api/v1/games/{id}` | Solo publicado; borrador → `404`. |
| `GET /api/v1/admin/games?publicationStatus=DRAFT` | ADMIN; paginado; sin filtro devuelve todos los estados. |
| `GET /api/v1/admin/games/{id}` | ADMIN; cualquier estado. |
| `POST /api/v1/admin/games` | ADMIN; crea siempre `DRAFT`; `201`. |
| `PUT /api/v1/admin/games/{id}` | ADMIN; actualiza información; `200`. |
| `PUT /api/v1/admin/games/{id}/publication` | ADMIN; publica/despublica; `200`. |
| `DELETE /api/v1/admin/games/{id}` | ADMIN; borrado físico y limpieza de medios; `204`. |

Los `POST/PUT/DELETE` actuales pueden mantenerse temporalmente delegando a los mismos casos de uso. Cambiar su semántica de creación a borrador requiere comunicarlo.

**Creación/edición propuesta:**

```json
{
  "name": "Juego ejemplo",
  "description": "Descripción del videojuego.",
  "jam": false,
  "developerCount": 1,
  "genreIds": [1],
  "developmentStatus": "IN_DEVELOPMENT"
}
```

**Publicación:**

```json
{
  "publicationStatus": "PUBLISHED"
}
```

Despublicar utiliza `DRAFT`, conservando versiones, enlaces y medios. La operación debe ser idempotente.

**Respuesta propuesta:**

```json
{
  "id": 42,
  "name": "Juego ejemplo",
  "description": "Descripción del videojuego.",
  "jam": false,
  "developerCount": 1,
  "genreIds": [1],
  "publicationStatus": "DRAFT",
  "developmentStatus": "IN_DEVELOPMENT",
  "cover": null,
  "screenshots": []
}
```

### Medios

| Endpoint propuesto | Contrato |
|---|---|
| `PUT /api/v1/admin/games/{id}/cover` | Multipart `file`, `altText`; crea/reemplaza portada. |
| `DELETE /api/v1/admin/games/{id}/cover` | Elimina portada. |
| `POST /api/v1/admin/games/{id}/screenshots` | Multipart `file`, `altText`; añade captura; `201`. |
| `PUT /api/v1/admin/games/{id}/screenshots/{mediaId}` | JSON con `altText`. |
| `PUT /api/v1/admin/games/{id}/screenshots/order` | `{"ids":[12,10,11]}`; todos deben pertenecer al juego. |
| `DELETE /api/v1/admin/games/{id}/screenshots/{mediaId}` | Elimina captura. |
| `GET /api/v1/games/{id}/media/{mediaId}/content` | Binario solo si el juego está publicado. |
| `GET /api/v1/admin/games/{id}/media/{mediaId}/content` | Binario para ADMIN, incluido borrador. |

Objeto de medio:

```json
{
  "id": 12,
  "kind": "SCREENSHOT",
  "altText": "Pantalla de juego",
  "position": 0,
  "url": "/api/v1/games/42/media/12/content"
}
```

La respuesta administrativa devolvería la ruta administrativa correspondiente. La clave física no necesita exponerse.

**Pendiente de decisión:** requisitos para publicar —por ejemplo portada y descripción completas— y si se permiten borradores incompletos. El contrato actual obliga a rellenar todos los campos de creación.

## 6. Esquema y migraciones mínimas

### Migraciones existentes

- [V1__create_initial_schema.sql](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/resources/db/migration/mysql/V1__create_initial_schema.sql): tablas de juegos, géneros, versiones, enlaces y hosts; cascadas de borrado.
- [V2__allow_shared_development_difficulty.sql](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/resources/db/migration/mysql/V2__allow_shared_development_difficulty.sql): permite compartir dificultad.
- [V3__remove_development_difficulty.sql](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/resources/db/migration/mysql/V3__remove_development_difficulty.sql): elimina dificultad.
- [V4__simplify_game_version_platform_processor.sql](C:/Users/nitro/Documents/programming/spring-boot/nitros-games-backend/src/main/resources/db/migration/mysql/V4__simplify_game_version_platform_processor.sql): plataforma/procesador pasan a relaciones independientes.

**La antigua dificultad no representa estado de desarrollo.**

### Nuevas migraciones propuestas

**V5 — Estados editoriales**

- `gamedata.publication_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT'`.
- `gamedata.development_status VARCHAR(32)` con valores y estrategia de relleno definidos.
- Índice `(publication_status, id)`.
- Restricciones de valores permitidos.
- Si se amplía la descripción: cambiar `descripcion VARCHAR(30)` por `TEXT` y ajustar DTO/mapeo JPA.

Para los registros existentes, el código no permite inferir qué debería estar publicado ni su estado de desarrollo. Evitar asignarlos automáticamente sin una política explícita. Un relleno inicial de publicación con `DRAFT` es conservador, pero ocultará el catálogo existente.

**V6 — Medios de juegos**

Nueva tabla `game_media`:

```text
id             BIGINT PK AUTO_INCREMENT
game_id        BIGINT NOT NULL FK → gamedata(id)
kind           VARCHAR(16) NOT NULL
storage_key    VARCHAR(255) NOT NULL UNIQUE
content_type   VARCHAR(100) NOT NULL
alt_text       VARCHAR(255) NOT NULL
position       INT NOT NULL
```

Añadir:

- Índice `(game_id, kind, position, id)`.
- Restricciones para `kind` y posición no negativa.
- Unicidad condicional de portada por juego, por ejemplo mediante columna generada e índice único.
- Política de borrado de filas vinculada al juego.

La cascada SQL **no elimina archivos**. El servicio debe recoger las claves antes del borrado y coordinar su limpieza posterior.

## 7. Orden recomendado

1. **Cerrar contratos:** estados de desarrollo, borradores incompletos, requisitos de publicación y política para datos existentes.
2. **Añadir estados y consultas diferenciadas**, junto con protección administrativa y filtros en todos los accesos públicos.
3. **Implementar publicar/despublicar** y adaptar DTOs.
4. **Añadir modelo de medios y almacenamiento reutilizable**, incluyendo lectura autorizada.
5. **Implementar portada, capturas, orden y limpieza al borrar.**
6. **Ajustar configuración local y errores** para el origen y cliente OAuth reales de Next.js.
7. **Verificar posteriormente** autorización, ausencia de filtraciones de borradores, migraciones y fallos de almacenamiento. No se ejecutaron esas verificaciones en esta auditoría.

## 8. Riesgos y compatibilidad

1. **Filtración de borradores:** hay que cubrir listado completo, paginado, búsqueda, detalle, versiones, enlaces, medios y `HEAD`. Filtrar únicamente el catálogo principal es insuficiente.
2. **Reglas de seguridad:** `/admin/**` debe protegerse antes del permiso genérico de lectura.
3. **Descripción demasiado corta:** 30 caracteres tanto en DTO como en SQL; ampliar solo uno no resuelve el problema.
4. **Medios privados:** una ruta administrativa con Bearer no se puede usar directamente en un `<img>` convencional añadiendo una cabecera. El contrato requiere transporte autenticado desde el consumidor; no se inspeccionó su implementación.
5. **Despublicación y caché:** ocultar en BD no retira copias ya cacheadas. Para medios de borradores, usar respuestas privadas/no almacenables; la caché del consumidor queda **UNKNOWN**.
6. **Diferencias local/runtime:** OSIV activo en local puede ocultar cargas diferidas que fallen con runtime.
7. **Pérdidas de actualizaciones:** el modelo inspeccionado no tiene `@Version`; ediciones administrativas concurrentes pueden sobrescribirse. Considerar versión optimista si habrá varios editores.
8. **Consistencia de archivos:** la limpieza actual registra fallos, pero no dispone de reintentos duraderos. Una caída puede dejar objetos huérfanos.
9. **Edición de publicados:** con el modelo mínimo propuesto, editar un publicado cambia inmediatamente su contenido público. Mantener una revisión pendiente sin alterar lo publicado requeriría otro modelo.
10. **Borrado real:** elimina versiones y enlaces; despublicar debe permanecer como operación independiente y no destructiva.

**Conclusión:** la integración puede reutilizar el CRUD, DTOs, transacciones, JWT, paginación y errores. El trabajo principal está en introducir estados independientes, controlar visibilidad en toda la jerarquía y crear medios propios de juegos con entrega de contenido autorizada.
