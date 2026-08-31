# Nitros Games Backend

[![CI](https://github.com/Nitros64/nitros-games-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/Nitros64/nitros-games-backend/actions/workflows/ci.yml)

API REST de `nitrosgames64.com` para administrar juegos, versiones, enlaces de
descarga, catálogos técnicos, herramientas de programación e imágenes de
servicios de alojamiento.

El proyecto es un monolito modular: se compila y despliega como una sola
aplicación Spring Boot, pero el código se organiza por capacidades de negocio
con límites comprobados mediante pruebas de arquitectura.

## Contenido

- [Tecnologías y características](#tecnologías-y-características)
- [Arquitectura](#arquitectura)
- [Inicio rápido con Docker](#inicio-rápido-con-docker)
- [Desarrollo local](#desarrollo-local)
- [API HTTP](#api-http)
- [Seguridad](#seguridad)
- [Base de datos](#base-de-datos)
- [Almacenamiento de imágenes](#almacenamiento-de-imágenes)
- [Pruebas](#pruebas)
- [Observabilidad y operación](#observabilidad-y-operación)
- [Documentación adicional](#documentación-adicional)

## Tecnologías y características

- Java 21 y Spring Boot 4.1.1.
- Spring MVC, Bean Validation y contratos HTTP basados en DTOs.
- Spring Data JPA con Hibernate; las entidades no se exponen en la API.
- MySQL 8.4.11 en producción y Flyway como propietario del esquema.
- Spring Security OAuth2 Resource Server stateless con JWT y roles de Keycloak.
- Errores uniformes mediante RFC Problem Details (`application/problem+json`).
- Subida segura de imágenes PNG, JPEG y GIF con verificación de firma.
- Actuator, métricas Prometheus, logs JSON y correlación con `X-Request-ID`.
- Pruebas rápidas con H2 y pruebas reales de migración con MySQL/Testcontainers.
- Imagen Docker multi-stage, usuario sin privilegios y filesystem raíz de solo
  lectura en Docker Compose.

## Arquitectura

```text
com.nitros64.nitros_games_backend
├── catalog         géneros, dificultades, plataformas y procesadores
├── game            juegos, versiones y enlaces de descarga
├── tooling         lenguajes, herramientas y compatibilidades
├── storage         metadatos y archivos de imágenes de hosts
├── security        autenticación, autorización y CORS
├── observability   correlación de peticiones y contexto de logging
└── shared          contratos técnicos compartidos
```

Los módulos funcionales siguen esta dirección general:

```text
api  →  application  →  domain
             ↓
        persistence
```

- `api` contiene controladores, DTOs y mappers HTTP.
- `application` implementa casos de uso, transacciones y resolución de
  dependencias entre módulos.
- `domain` contiene las entidades y sus operaciones de negocio.
- `persistence` contiene exclusivamente repositorios Spring Data JPA.

Los tests de `src/test/java/.../architecture` protegen estos límites, los
mapeos JPA y la ubicación de los componentes compartidos.

## Inicio rápido con Docker

Este es el camino recomendado: no requiere instalar Java, Maven ni MySQL en el
host; solo Docker con Compose.

1. Crea la configuración local:

   ```powershell
   Copy-Item .env.example .env
   ```

   En Bash:

   ```shell
   cp .env.example .env
   ```

2. Edita `.env` y sustituye `DB_PASSWORD`, `DB_ROOT_PASSWORD`,
   `KEYCLOAK_ADMIN_PASSWORD` y `NITROS_GAMES_CLI_SECRET` por valores aleatorios
   diferentes. Estos valores son secretos locales y no deben versionarse.

3. Valida la configuración e inicia MySQL, Keycloak y la API:

   ```shell
   docker compose --profile identity config --quiet
   docker compose --profile identity up -d --build --wait
   docker compose ps
   ```

4. Comprueba que la API está lista:

   ```shell
   curl --fail http://localhost:8080/actuator/health/readiness
   ```

   En PowerShell también puedes usar:

   ```powershell
   Invoke-RestMethod http://localhost:8080/actuator/health/readiness
   ```

La API queda disponible en `http://localhost:8080` y Keycloak en
`http://localhost:8081`. MySQL no publica su puerto al host. Los datos de MySQL
y las imágenes se conservan en los volúmenes `mysql-data` y `host-images`.

Para consultar logs o detener los contenedores sin borrar datos:

```shell
docker compose logs -f api
docker compose --profile identity down
```

`docker compose down --volumes` elimina permanentemente la base de datos y las
imágenes almacenadas; úsalo solo con datos desechables.

## Desarrollo local

El repositorio incluye Maven Wrapper, por lo que no es necesario instalar
Maven. Para ejecutar la aplicación fuera de Docker necesitas JDK 21 y una
instancia MySQL accesible.

Puedes iniciar la base de datos y el proveedor de identidad de desarrollo con
Docker:

```shell
docker run --name nitros-games-mysql-dev --detach --publish 3306:3306 \
  --env MYSQL_DATABASE=nitrosgames \
  --env MYSQL_USER=nitros \
  --env MYSQL_PASSWORD=local-db-password \
  --env MYSQL_ROOT_PASSWORD=local-root-password \
  mysql:8.4.11

docker compose --profile identity up -d keycloak
```

Configura después el perfil `local` y arranca Spring Boot.

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_PASSWORD = "local-db-password"
$env:OAUTH2_ISSUER_URI = "http://localhost:8081/realms/nitros-games"
$env:OAUTH2_JWK_SET_URI = "http://localhost:8081/realms/nitros-games/protocol/openid-connect/certs"
$env:OAUTH2_AUDIENCE = "nitros-games-api"
.\mvnw.cmd spring-boot:run
```

Bash:

```shell
export SPRING_PROFILES_ACTIVE=local
export DB_PASSWORD=local-db-password
export OAUTH2_ISSUER_URI=http://localhost:8081/realms/nitros-games
export OAUTH2_JWK_SET_URI=http://localhost:8081/realms/nitros-games/protocol/openid-connect/certs
export OAUTH2_AUDIENCE=nitros-games-api
./mvnw spring-boot:run
```

El perfil local usa por defecto `jdbc:mysql://localhost:3306/nitrosgames`, el
usuario `nitros`, almacenamiento en `uploadImageFileHost` y origen CORS
`http://localhost:4200`. Spring Boot no carga archivos `.env` automáticamente;
Compose sí los utiliza.

La configuración completa se describe en
[docs/configuration.md](docs/configuration.md).

## API HTTP

### Recursos principales

| Recurso | Ruta canónica |
| --- | --- |
| Juegos | `/api/v1/games` |
| Géneros | `/api/v1/game-genres` |
| Dificultades | `/api/v1/development-difficulties` |
| Plataformas | `/api/v1/platforms` |
| Procesadores | `/api/v1/processors` |
| Lenguajes | `/api/v1/programming-languages` |
| Tipos de herramienta | `/api/v1/programming-tool-types` |
| Herramientas | `/api/v1/programming-tools` |
| Imágenes de hosts | `/api/v1/server-host-images` |

Las rutas históricas sin plural o sin guiones siguen disponibles temporalmente
como alias de compatibilidad. Todo cliente nuevo debe usar las rutas canónicas;
las cabeceras `Location` siempre apuntan a ellas.

### Operaciones comunes

Los catálogos y los recursos de `tooling` soportan:

| Método | Ruta | Resultado |
| --- | --- | --- |
| `GET` | `/recurso` | Lista completa |
| `GET` | `/recurso/paged?page=0&size=20` | Página estable de resultados |
| `GET` | `/recurso/search?...` | Búsqueda paginada |
| `GET` | `/recurso/{id}` | Un recurso |
| `POST` | `/recurso` | Crea uno; devuelve `201` y `Location` |
| `POST` | `/recurso/batch` | Crea varios |
| `PUT` | `/recurso/{id}` | Sustituye los datos editables |
| `DELETE` | `/recurso/{id}` | Elimina y devuelve `204` |

El tamaño de página máximo aceptado es 100. Los filtros de búsqueda de juegos
y herramientas son opcionales y combinables:

```text
GET /api/v1/games/search?name=nitro&genreId=1&jam=false
    &page=0&size=20&sort=name,asc

GET /api/v1/programming-tools/search?name=gradle&toolTypeId=1
    &languageId=1&page=0&size=20&sort=name,asc
```

### Ejemplo: obtener un token y crear un género

Para pruebas locales, el cliente confidencial `nitros-games-cli` usa el flujo
`client_credentials`. Su secreto es el valor local de
`NITROS_GAMES_CLI_SECRET`:

```powershell
$env:NITROS_GAMES_CLI_SECRET = "el-mismo-valor-configurado-en-.env"

$token = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/realms/nitros-games/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "client_credentials"
    client_id = "nitros-games-cli"
    client_secret = $env:NITROS_GAMES_CLI_SECRET
  }

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/game-genres" `
  -Headers @{ Authorization = "Bearer $($token.access_token)" } `
  -ContentType "application/json" `
  -Body '{"name":"Strategy"}'
```

La petición HTTP equivalente es:

```shell
curl --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{"name":"Strategy"}' \
  http://localhost:8080/api/v1/game-genres
```

Respuesta:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/v1/game-genres/1
Content-Type: application/json

{"id":1,"name":"Strategy"}
```

### Juegos y recursos anidados

Las versiones pertenecen a un juego y los enlaces de descarga pertenecen a
una versión:

```text
/api/v1/games/{gameId}/versions
/api/v1/games/{gameId}/versions/{versionId}
/api/v1/games/{gameId}/versions/{versionId}/download-links
/api/v1/games/{gameId}/versions/{versionId}/download-links/{linkId}
```

Las peticiones envían identificadores de recursos relacionados, nunca objetos
JPA. Por ejemplo, para crear un juego:

```json
{
  "name": "Nitro Game",
  "description": "Great game",
  "jam": false,
  "developerCount": 2,
  "genreIds": [1, 2]
}
```

### Errores

Todos los errores usan `application/problem+json` y contienen un código estable
para clientes:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more request fields are invalid",
  "instance": "/api/v1/game-genres",
  "code": "validation_failed",
  "errors": [
    {
      "field": "name",
      "code": "Size",
      "message": "el tamaño tiene que estar entre 4 y 30"
    }
  ]
}
```

Los valores rechazados, mensajes de base de datos y trazas nunca se incluyen en
la respuesta.

## Seguridad

- `GET` y `HEAD` bajo `/api/**` son públicos.
- `POST`, `PUT` y `DELETE` requieren un JWT con el rol de realm `ADMIN`.
- La aplicación es stateless y no crea sesiones de autenticación.
- CORS usa una allowlist explícita; los comodines están rechazados.
- Actuator health es público y `/actuator/prometheus` requiere administrador.
- El emisor, las claves JWK y la audiencia del token se validan explícitamente.
- En producción, los tokens deben enviarse exclusivamente mediante HTTPS.

El realm de desarrollo incluye un cliente público `nitros-games-web` preparado
para Authorization Code con PKCE y el cliente operacional
`nitros-games-cli`. La API acepta proveedores OIDC compatibles configurando:

```text
OAUTH2_ISSUER_URI
OAUTH2_JWK_SET_URI
OAUTH2_AUDIENCE
APP_SECURITY_ALLOWED_ORIGINS
```

El Keycloak de Compose ejecuta `start-dev`: facilita desarrollo y entrevistas,
pero producción debe usar un proveedor OIDC gestionado o una instalación de
Keycloak endurecida, persistente y publicada detrás de HTTPS.

## Base de datos

Flyway es el único propietario de la evolución del esquema. Al iniciar:

1. Flyway valida y aplica las migraciones pendientes.
2. Hibernate valida que las entidades coincidan con el esquema mediante
   `ddl-auto=validate`.
3. Hibernate nunca crea ni actualiza tablas en `local` o `prod`.

Las migraciones están en `src/main/resources/db/migration/mysql`. Una migración
publicada no debe editarse: todo cambio posterior requiere un archivo versionado
nuevo. Consulta [docs/database-migrations.md](docs/database-migrations.md) antes
de adoptar una base existente.

## Almacenamiento de imágenes

La creación y modificación de imágenes usa `multipart/form-data`:

| Operación | Ruta canónica | Campos |
| --- | --- | --- |
| Crear | `POST /api/v1/server-host-images` | `name`, `fileHostImage` |
| Reemplazar imagen | `PUT /api/v1/server-host-images/{id}/image` | `name`, `fileHostImage` |
| Cambiar nombre | `PUT /api/v1/server-host-images/{id}/name` | `name` |
| Eliminar | `DELETE /api/v1/server-host-images/{id}` | — |

Ejemplo:

```shell
curl --header "Authorization: Bearer $ACCESS_TOKEN" \
  --form "name=MediaFire" \
  --form "fileHostImage=@mediafire.png;type=image/png" \
  http://localhost:8080/api/v1/server-host-images
```

El servidor genera el nombre físico, limita el tamaño, comprueba el MIME y la
firma binaria, impide salir del directorio configurado y coordina los archivos
con la transacción de base de datos. Producción requiere un volumen persistente
en `APP_STORAGE_HOST_IMAGES_DIRECTORY`.

## Pruebas

Suite rápida y autocontenida con H2:

```powershell
.\mvnw.cmd clean verify
```

En Bash:

```shell
./mvnw clean verify
```

Verificación completa con un MySQL 8.4.11 desechable mediante Testcontainers:

```powershell
.\mvnw.cmd clean verify -Pmysql-it
```

Docker debe estar activo para `mysql-it`. Esta verificación ejecuta las
migraciones Flyway, arranca Hibernate contra el esquema MySQL real y comprueba
consultas de repositorio, incluidas las cargas detalladas y jerárquicas de
`game`. La suite H2 también controla el número de sentencias de estas consultas
para detectar regresiones N+1.

El workflow `CI` ejecuta en cada `push` y pull request hacia `main` las
validaciones de Terraform, la suite H2, la verificación MySQL/Testcontainers y
la construcción local de la imagen Docker. CI no recibe credenciales AWS ni
publica o despliega imágenes.

El workflow `CD - Staging` se inicia manualmente y recibe el SHA completo de un
commit integrado en `main`. Antes de publicar en ECR comprueba que ese commit
tenga un CI exitoso y que la instancia de staging esté online en Systems
Manager. Consulta [la guía de staging](infra/terraform/staging/README.md) para
iniciarlo y detener la infraestructura cuando no se use.

## Observabilidad y operación

| Función | Endpoint o comportamiento |
| --- | --- |
| Liveness | `/actuator/health/liveness` |
| Readiness, incluida la BD | `/actuator/health/readiness` |
| Métricas protegidas | `/actuator/prometheus` |
| Correlación | Cabecera `X-Request-ID` |
| Logs de producción | Un objeto JSON Logstash por línea |

Si el cliente envía un `X-Request-ID` válido, la API lo conserva; de lo
contrario genera un UUID. La misma identificación aparece en la respuesta y en
el contexto de logging.

El despliegue Compose incluye health checks, rotación de logs, límites de
memoria y procesos, shutdown ordenado, usuario no root y volúmenes persistentes.
Consulta [docs/docker.md](docs/docker.md) para los detalles de producción.

## Documentación adicional

- [Configuración y perfiles](docs/configuration.md)
- [Docker y despliegue](docs/docker.md)
- [Migraciones de base de datos](docs/database-migrations.md)
- [Migración a Spring Boot 4](docs/spring-boot-4-migration.md)
- [Baseline de compilación](docs/build-baseline.md)

## Flujo de cambios

Los cambios se desarrollan en una rama descriptiva, se validan con H2 y MySQL,
y se fusionan únicamente después de que las pruebas pasen. No deben incluirse
contraseñas, archivos `.env`, imágenes subidas, datos locales ni artefactos de
`target` en los commits.
