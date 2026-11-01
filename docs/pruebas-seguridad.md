# Pruebas de seguridad y contratos

Auditoría limitada: se conservan los tests existentes; no se modifican contratos ni código de producción.

| Área | Cobertura previa reutilizada | Ampliación |
|---|---|---|
| Seguridad HTTP | 401 sin token/token malformado, 403 sin ADMIN, ADMIN permitido, GET público, CORS | Bearer real en el filtro con JwtDecoder simulado; conversor configurado de Keycloak, rol malformado y rechazo del decoder |
| Confianza JWT | Issuer, cliente, audiencia/scope, tipo de token y claims inválidos | Caducidad y not-before |
| Publicación | DRAFT público 404, ADMIN 200 en `/admin/games`, PUBLISHED 200, listados/paginación/búsqueda, idempotencia | HEAD y conservación explícita de IDs de versiones/descargas al despublicar |
| Validación | Validaciones puntuales de DTO, JSON malformado, IDs de rutas, referencias y jerarquías | Campos aislados ausentes/null, blanco, mínimo/máximo, límites válidos, IDs cero/negativos y elementos inválidos de genreIds |
| Integridad | LanguageTool duplicado/en uso, descarga duplicada, jerarquía de versiones | Nombres duplicados en los cinco catálogos; atributos únicos de herramientas; referencias desconocidas sin escrituras parciales |

`JwtTestSupport` usa `ROLE_ADMIN`/`ROLE_USER`. El conversor real transforma `realm_access.roles` a esos mismos authorities (normaliza mayúsculas). `BearerAuthorizationTests` evita el atajo `jwt()` para comprobar el cableado real de Spring Security y solo simula el decoder, sin Keycloak ni red. Los tests de confianza validan los claims y fechas por separado; no sustituyen una prueba completa de OAuth2/PKCE.

El contrato actual devuelve `400 malformed_json` para primitivas ausentes/null (`jam`, `developerCount`) y URL no deserializable. Las restricciones Bean Validation devuelven `400 validation_failed`. No se añaden restricciones nuevas, por ejemplo validación de formato URL al campo `link`, que actualmente es un String con límites de longitud.

Ejecución completa:

```powershell
.\mvnw.cmd clean verify -Pmysql-it
```

H2 usa el perfil `test`; las integraciones MySQL usan Testcontainers. No requieren Keycloak ni la BD local real. No hay cambios de dependencias.

Postman: importar `postman/NitrosGames.postman_collection.json`. Contiene 58 peticiones ordenadas por recursos con comprobaciones HTTP y captura de IDs. Usar exclusivamente una BD de pruebas vacía; configurar `base_url`, `admin_token`, `user_token` (sin ADMIN) y `host_image_id` de un host de imágenes preparado previamente. Los demás IDs se capturan al crear recursos. Los tokens se obtienen externamente con Keycloak/PKCE y no se guardan en Git. La última carpeta elimina los recursos creados, excepto el host preparado previamente.

La colección se valida estáticamente (JSON, variables, cuerpos y sintaxis de scripts); no se ejecuta automáticamente contra servicios locales reales.
