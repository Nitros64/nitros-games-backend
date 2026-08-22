# Migración a Spring Boot 4

## Alcance

Esta fase actualiza el proyecto de Spring Boot 3.4.3 a 4.1.1 y mantiene Java 21. No cambia todavía la arquitectura funcional, el modelo de dominio ni los contratos HTTP.

## Cambios de compatibilidad

- Se actualizó el parent de Maven a `spring-boot-starter-parent` 4.1.1.
- Se añadió `spring-boot-starter-webmvc-test`, requerido por la modularización de los starters de pruebas web de Spring Boot 4.
- `AutoConfigureMockMvc` se migró al paquete `org.springframework.boot.webmvc.test.autoconfigure`.
- La creación de errores de validación dejó de depender de clases internas de Hibernate Validator y utiliza únicamente la API pública de Jakarta Validation.
- La respuesta para archivos demasiado grandes ahora utiliza HTTP 413 (`CONTENT_TOO_LARGE`) en vez del incorrecto 509.
- Se eliminó del perfil de pruebas el dialecto H2 explícito; Hibernate lo detecta desde la conexión.
- Se corrigieron construcciones genéricas sin parametrizar que el compilador señalaba durante la migración.

## Dependencias efectivas principales

Las versiones son administradas por Spring Boot 4.1.1:

| Componente | Versión |
| --- | --- |
| Spring Framework | 7.0.9 |
| Spring Data JPA | 4.1.1 |
| Hibernate ORM | 7.4.5.Final |
| Hibernate Validator | 9.1.3.Final |
| Tomcat embebido | 11.0.24 |
| MySQL Connector/J | 9.7.0 |

## Verificación

Ejecutar desde la raíz del repositorio:

```powershell
.\mvnw.cmd clean verify
```

La migración se validó con Java 21, carga completa del contexto Spring, 14 repositorios JPA, esquema H2 en memoria, MockMvc y las pruebas unitarias existentes.

## Riesgos y trabajo posterior

- La validación actual usa H2; falta una prueba de integración contra MySQL real, preferiblemente con Testcontainers.
- La aplicación todavía no incorpora Spring Security. Esta migración no expone ni protege endpoints.
- El aviso de Mockito sobre la carga dinámica de su agente no falla bajo Java 21, pero debe resolverse antes de adoptar un JDK que la bloquee por defecto.
- Siguen pendientes la modularización por dominios, las migraciones versionadas de base de datos y la revisión de contratos y excepciones indicada en la evaluación arquitectónica.
