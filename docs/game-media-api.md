# Game media API

Los contratos existentes de Game, GameVersion y DownloadLink no cambian. La portada sigue siendo opcional para publicar.

## Rutas

Base administrativa: `/api/v1/admin/games/{gameId}/media`. Todas las operaciones requieren ADMIN.

| Método | Sufijo | Resultado |
|---|---|---|
| GET | vacío | 200, listado de portada y screenshots, también para DRAFT |
| GET | `/{mediaId}/content` | 200, bytes de la imagen |
| POST | `/cover` | 201, crea portada; 409 si ya existe |
| PUT | `/cover` | 200, sustituye portada conservando ID; 404 si no existe |
| DELETE | `/cover` | 204; 404 si no existe |
| POST | `/screenshots` | 201, añade una captura al final |
| PUT | `/{mediaId}/metadata` | 200, actualiza altText y caption de portada o screenshot |
| PUT | `/screenshots/order` | 200, establece el orden completo de las capturas |
| DELETE | `/screenshots/{mediaId}` | 204, elimina captura y compacta posiciones |

Rutas públicas:
- `GET /api/v1/games/{gameId}/media`
- `GET /api/v1/games/{gameId}/media/{mediaId}/content`

Las rutas públicas devuelven 404 para DRAFT, incluso con token ADMIN. Para consultar un DRAFT se usa la ruta administrativa. También se comprueba la pertenencia del medio al juego. Los listados y contenidos usan `Cache-Control: no-store, private` para evitar reutilizar imágenes cacheadas después de despublicar. No hay URLs públicas de disco o de almacenamiento.

## Peticiones y respuestas

Las subidas POST y PUT usan `multipart/form-data`:
- `file`: archivo obligatorio.
- `altText`: opcional, máximo 200 caracteres; por defecto vacío.
- `caption`: opcional, máximo 500 caracteres; por defecto vacío.
- Al sustituir portada, los metadatos enviados reemplazan los anteriores; los omitidos quedan vacíos.

Metadatos, `application/json`; ambos campos obligatorios, permiten vacío:
```json
{"altText":"Menú principal","caption":"Captura del menú"}
```

Orden, `application/json`; debe incluir todos los IDs de screenshots del juego exactamente una vez, sin portada:
```json
{"screenshotIds":[14,12,13]}
```

Respuesta de subida/metadatos y elemento de los listados:
```json
{
  "id": 12,
  "gameId": 7,
  "type": "SCREENSHOT",
  "position": 1,
  "altText": "Menú principal",
  "caption": "Captura del menú",
  "contentType": "image/png",
  "byteSize": 15428,
  "width": 1280,
  "height": 720,
  "contentUrl": "/api/v1/admin/games/7/media/12/content"
}
```

En la consulta pública, `contentUrl` usa el prefijo público. Portada: posición 0. Screenshots: posiciones consecutivas desde 1. Las subidas 201 incluyen Location apuntando al contenido. Los errores mantienen Problem Details: 400 validación/orden/nombre, 401/403 autenticación/autorización, 404 recurso no visible o inexistente, 409 portada duplicada, 413 límites, 415 formato/contenido no válido.

## Almacenamiento y migración

`V6__create_game_media.sql` crea únicamente `game_media`, asociada a `gamedata`. La restricción única por juego/tipo/posición y el CHECK de posiciones garantizan una sola portada. La clave del archivo es única y generada por el servidor. La migración no modifica videojuegos existentes.

Filesystem es el adaptador inicial, independiente de ServerHostImage:
- `app.storage.game-media.directory`: `game-media` por defecto (ignorado por Git).
- `app.storage.game-media.max-bytes`: 10485760 (10 MiB).
- `app.storage.game-media.max-pixels`: 20000000.
- También se aplica el límite multipart global existente.

Solo PNG y JPEG. Se valida nombre seguro (máximo 120 caracteres), extensión, MIME, decodificación real y dimensiones antes de decodificar los píxeles. Se recodifica la imagen para descartar metadatos y contenido añadido. No se aceptan SVG, GIF o WebP. No servir el directorio mediante un servidor estático.

`GameMediaStorage` permite añadir posteriormente un adaptador S3 privado sin cambiar los servicios. No se ha activado ni configurado S3 para medios; los adaptadores de host-images siguen intactos. Un futuro adaptador debe mantener los objetos privados y devolver contenido a través del puerto, sin URLs públicas permanentes.

Las mutaciones bloquean la fila del juego. El nuevo archivo se elimina en rollback; el anterior se elimina después del commit. El borrado de juegos mediante la API también elimina sus medios y programa la limpieza. BD y filesystem no forman una transacción distribuida: un fallo de disco o caída del proceso durante la limpieza puede dejar un archivo huérfano; los fallos de borrado se registran para reintento operativo. No se añade una cola de reintentos.

## Verificación

`./mvnw.cmd verify -Pmysql-it`: suite existente, contratos HTTP de medios en H2 y MySQL/Testcontainers, restricciones de V6, concurrencia, rollback y pruebas del adaptador filesystem. Los archivos de pruebas se guardan en directorios exclusivos bajo target o temporales; no se usa la BD local real.
