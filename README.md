# Nitros Games Backend

API REST de `nitrosgames64.com` para administrar juegos, versiones, enlaces de
descarga, catálogos técnicos, herramientas de programación e imágenes de los
servicios de alojamiento.

El proyecto se está evolucionando como un **monolito modular**: se despliega
como una única aplicación Spring Boot, pero el código está organizado por
capacidades de negocio con límites explícitos entre módulos.

## Estado actual

- Java 21 y Spring Boot 4.1.
- MySQL 8.4 como base de datos de producción.
- JPA/Hibernate con esquema validado, nunca generado en producción.
- Migraciones versionadas con Flyway.
- API stateless protegida con Spring Security.
- Errores HTTP con `application/problem+json`.
- Subida segura de imágenes PNG, JPEG y GIF.
- Contenedores sin privilegios y almacenamiento persistente con Docker Compose.
- Health checks, métricas Prometheus, logs JSON y correlación mediante
  `X-Request-ID`.
- Pruebas rápidas con H2 y pruebas de integración con MySQL mediante
  Testcontainers.

## Arquitectura

```text
com.nitros64.nitros_games_backend
├── catalog         catálogos compartidos: géneros, plataformas y procesadores
├── game            juegos, versiones y enlaces de descarga
├── tooling         lenguajes, herramientas, tipos y compatibilidades
├── storage         metadatos y archivos de imágenes de hosts
├── security        autenticación, autorización y CORS
├── observability   correlación de peticiones
└── shared          contratos técnicos realmente compartidos
```

Cada módulo funcional utiliza las capas `api`, `application`, `domain` y
`persistence` cuando las necesita. Los controladores trabajan con DTOs; las
entidades JPA no forman parte del contrato HTTP.

## Requisitos

- Docker Desktop o Docker Engine con Compose, opción recomendada.
- Para ejecutar sin contenedores: JDK 21 y MySQL 8.4.

No es necesario instalar Maven: el repositorio incluye Maven Wrapper.

## Inicio rápido con Docker

1. Crea el archivo local de configuración:

   ```powershell
   Copy-Item .env.example .env
   ```

   En Bash:

   ```shell
   cp .env.example .env
   ```

2. Sustituye en `.env` todas las contraseñas de ejemplo por valores aleatorios
   distintos.

3. Construye e inicia la API y MySQL:

   ```shell
   docker compose up -d --build --wait
   docker compose ps
   ```

4. Comprueba la disponibilidad:

   ```shell
   curl --fail http://localhost:8080/actuator/health/readiness
   ```

La API queda disponible en `http://localhost:8080`. MySQL no publica ningún
puerto al host. Para detener el stack sin borrar datos:

```shell
docker compose down
```

No ejecutes `docker compose down --volumes` salvo que quieras eliminar de forma
permanente la base de datos y las imágenes almacenadas.

## Desarrollo local

Con una instancia MySQL accesible, configura el perfil `local`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_PASSWORD = "your-local-password"
$env:APP_SECURITY_ADMIN_PASSWORD = "a-long-random-local-password"
.\mvnw.cmd spring-boot:run
```

La lista completa de variables, perfiles y decisiones de seguridad está en
[docs/configuration.md](docs/configuration.md). La guía de despliegue se
encuentra en [docs/docker.md](docs/docker.md).

## API

Lecturas `GET` y `HEAD` bajo `/api/**` son públicas. Las operaciones `POST`,
`PUT` y `DELETE` requieren autenticación HTTP Basic con el usuario
administrador configurado. En producción, las credenciales deben viajar
exclusivamente sobre HTTPS.

Recursos principales:

| Módulo | Recurso base |
| --- | --- |
| Juegos | `/api/v1/games` |
| Géneros | `/api/v1/gamegenre` |
| Dificultades | `/api/v1/developmentdifficulty` |
| Plataformas | `/api/v1/platform` |
| Procesadores | `/api/v1/processor` |
| Lenguajes | `/api/v1/programlanguages` |
| Tipos de herramienta | `/api/v1/programtooltypes` |
| Herramientas | `/api/v1/programmingtools` |
| Imágenes de hosts | `/api/v1/serverhostimage` |

Los catálogos y el módulo `tooling` permiten búsquedas paginadas por nombre:

```text
GET /api/v1/gamegenre/search?name=strategy&page=0&size=20
GET /api/v1/developmentdifficulty/search?name=advanced&page=0&size=20
GET /api/v1/platform/search?name=windows&page=0&size=20
GET /api/v1/processor/search?name=arm&page=0&size=20
GET /api/v1/programlanguages/search?name=java&page=0&size=20
GET /api/v1/programtooltypes/search?name=compiler&page=0&size=20
GET /api/v1/programmingtools/search?name=gradle&toolTypeId=1
    &languageId=1&platformId=1&processorId=1&page=0&size=20&sort=name,asc
```

Todos los filtros de herramientas son opcionales y combinables. El servidor
limita cualquier página solicitada a 100 elementos.

## Base de datos

Flyway es el único propietario de la evolución del esquema. Hibernate utiliza
`ddl-auto=validate` fuera de las pruebas rápidas. Las migraciones MySQL están en
`src/main/resources/db/migration/mysql` y nunca deben editarse después de haber
sido publicadas; cada cambio de esquema requiere una migración nueva.

## Pruebas

Suite rápida con H2:

```powershell
.\mvnw.cmd clean verify
```

Suite completa, incluyendo MySQL 8.4 mediante Testcontainers:

```powershell
.\mvnw.cmd clean verify -Pmysql-it
```

En Bash sustituye `.\mvnw.cmd` por `./mvnw`. Docker debe estar activo para el
perfil `mysql-it`.

## Operación

- Liveness: `/actuator/health/liveness`
- Readiness con comprobación de base de datos:
  `/actuator/health/readiness`
- Métricas protegidas: `/actuator/prometheus`
- Correlación: envía opcionalmente `X-Request-ID`; la API lo devuelve y lo
  incorpora como `requestId` en los logs.

Prometheus requiere las credenciales del administrador actual. Los detalles de
health no se exponen y el endpoint de métricas debe restringirse al sistema de
monitorización.

## Flujo de cambios

Cada cambio se desarrolla en una rama descriptiva, se valida con H2 y MySQL y
se fusiona únicamente después de revisar sus pruebas. No deben incluirse
contraseñas, archivos `.env`, imágenes subidas ni datos locales en los commits.
