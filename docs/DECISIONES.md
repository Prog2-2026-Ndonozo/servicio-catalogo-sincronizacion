# Decisiones de diseño

Registro de decisiones (E §10 y §12). Cada decisión importante tiene su ADR en `docs/adr/NNNN-titulo.md`
(plantilla: `../../ADR-template.md`). Esta tabla es el índice: se lee primero, el ADR solo si hace falta el detalle.

## Tomadas

| Decisión | Resumen | Dónde |
|---|---|---|
| Arquitectura hexagonal, un solo módulo `catalog` | Tres capas + `config/`; dominio sin framework; reglas verificadas con ArchUnit | `arquitecturaHexagonalServicioCatalogo.md` |
| Motor de base de datos | PostgreSQL propio y exclusivo (E §3) | — |
| Estructura de capas | Fijada en issue #1; los fixtures de `arquitecturafixtures/` prueban que las reglas detectan violaciones | `arquitecturaHexagonalServicioCatalogo.md` |
| Modelo de dominio base: 4 conceptos | `ProfessionalCategory`, `Professional`, `WeeklySchedule`, `CatalogVersion` (referencia por ID: propuesta) | `adr/0010-modelo-de-dominio-base.md` |
| Incluir `createdAt`/`updatedAt` | Se conservan los datos que publica la cátedra | `adr/0011-incluir-created-at-y-updated-at.md` |
| Tipos de fechas y horas | `Instant`, `LocalTime`, `DayOfWeek` | `adr/0012-tipos-de-fechas-y-horas.md` |
| Tipos numéricos, textos y estado | IDs y versión `Long`/`long`; `slotDurationMinutes` `int`; textos `String`; `enabled` `boolean` | `adr/0013-tipos-numericos.md` |
| Validaciones del modelo | Nombres no vacíos; `startTime < endTime`; rango múltiplo de la duración; versión ≥ 0 (0 = inicial) | `adr/0014-validaciones-del-modelo.md` |
| Modelo inmutable que se valida al construirse | Clases Lombok sin setters ni constructor vacío, con builder; **desvío de la regla 2 del skill** | `adr/0015-modelo-inmutable-con-validacion.md` |
| Excepción de validación del modelo en `domain/` | Tipo propio, lanzado por el modelo; **desvío de la regla 1 de excepciones del skill** | `adr/0016-excepcion-de-validacion-del-dominio.md` |
| Flyway para las migraciones | SQL plano en `db/migration`; Hibernate en `ddl-auto: validate` | `adr/0017-flyway-para-migraciones-de-bd.md` |
| Claves foráneas diferidas y reglas de esquema | FK `DEFERRABLE INITIALLY DEFERRED`; versión local en la misma base; ID de la cátedra como PK; índices en las FK | `adr/0018-claves-foraneas-diferidas.md` |
| `RestClient` como cliente HTTP hacia la cátedra | Cliente síncrono de Spring; sin WebFlux; se prueba con `MockRestServiceServer` | `adr/0019-restclient-como-cliente-http-de-la-catedra.md` |
| Credenciales de la cátedra en el `.env` | `CATEDRA_BASE_URL` y `CATEDRA_JWT` vía `@ConfigurationProperties`; el JWT solo existe en `config/` y no se imprime | `adr/0020-credenciales-de-la-catedra-en-configuracion.md` |
| `CatalogSnapshot` como resultado del puerto | Inmutable, `long snapshotVersion` + 3 listas con copia defensiva; no reutiliza `CatalogVersion` | `adr/0021-catalogsnapshot-como-resultado-del-puerto.md` |
| Fallos de la cátedra como una sola excepción | `CatedraUnavailableException` en `domain/exception`, con `httpStatus` y causa; el caso de uso no distingue el tipo de fallo | `adr/0022-errores-de-la-catedra-como-una-sola-excepcion.md` |
| Alcance transaccional de la sincronización completa | `@Transactional` (Spring) en el caso de uso; categorías, profesionales, horarios y, al final, la versión; la llamada HTTP queda dentro de la transacción | `adr/0023-alcance-transaccional-de-la-sincronizacion-completa.md` |
| Sincronización completa inicial al arrancar | `StartupSyncRunner` → fachada `CatalogSyncService.synchronizeIfEmpty()` si la versión local es 0; se apaga con `catedra.sync-on-startup=false` | `adr/0024-sincronizacion-inicial-al-arrancar.md` |
| El servicio sigue activo si la cátedra no responde al arrancar | Se captura `CatedraUnavailableException` y se loguea; un dato inválido sí hace fallar el arranque | `adr/0025-el-servicio-sigue-activo-si-la-catedra-no-responde.md` |
| Datos inválidos de la cátedra | Se asume que llegan válidos; sin manejo especial. Si llega uno inválido, falla la unidad de trabajo y la versión local no avanza | `adr/0014-validaciones-del-modelo.md` |
| Versión 0 | No se publica (el profesor lo consideró improbable); `0` es el estado inicial, base vacía | `adr/0014-validaciones-del-modelo.md` |

## Pendientes

| Decisión | Bloquea | Notas |
|---|---|---|
| Idempotencia: deduplicar por `eventId` de Kafka (I §15.1, §18.1) | #6 | Falta decidir dónde se persiste el `eventId` procesado |
| Qué significa "disponibilidad" en el filtro de búsqueda | #9 (issue #8, ADR) | E §4.1 no lo define |
| Quién consume la búsqueda: ¿KMP directo + turnos, o solo turnos? | #9 | Si solo turnos, se elimina la fila de KMP de la tabla de puertos de entrada |
| Dónde viven registro/login del usuario final (E §3.2) | — | E no asigna el servicio; `reunion.md` lo ubica en catálogo. No está en el backlog |
| Propagación de identidad entre servicios (JWT usuario vs técnico) | #10 | `ADR-0002` aún no escrito (E §9) |
| Reintentos y reconstrucción de la sincronización (qué hacer con filas ausentes del snapshot) | #6 | Alcance de la transacción resuelto en ADR-0023; falta la política de reintentos y la limpieza previa |
| `SyncDiagnosticsPort`: tabla propia o sobre `CatalogRepositoryPort` | #7, #12 | Depende de la máquina de estados de sync |
| Significado de "nombre" en el filtro de búsqueda; efecto de una categoría deshabilitada sobre sus profesionales | #9 | Huecos de E §4.1 (ADR-0010) |
