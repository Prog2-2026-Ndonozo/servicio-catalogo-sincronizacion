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
| Listener Kafka de `CatalogUpdated` | Payload `String` parseado por nosotros; mensaje defectuoso se descarta con warning, fallo operativo se propaga (reintentos de Spring Kafka); `auto-offset-reset: earliest` | `adr/0026-listener-kafka-de-catalogupdated.md` |
| Lectura de Redis de la cátedra | Puerto `CatedraCatalogReadPort`; adaptador sobre `StringRedisTemplate` (`HGETALL` de metadata, `HMGET` por tipo); DTOs propios; fallo de Redis = `CatedraUnavailableException`, dato incoherente = `InvalidCatalogDataException`; timeout de 5 s | `adr/0027-lectura-de-redis-de-la-catedra.md` |
| Deduplicación de eventos por `eventId` | Tabla `processed_events` en PostgreSQL, escrita en la misma transacción que aplica el cambio; antes se descarta por `newVersion <=` versión local | `adr/0028-deduplicacion-de-eventos-por-event-id.md` |
| Respaldo con snapshot fuera de la ventana | `ApplyIncrementalChangeUseCaseImpl` lanza `FullSyncRequiredException` si la versión local es 0 o menor que `oldestAvailableVersion`; `CatalogSyncService` la captura y pide el snapshot completo | `adr/0029-respaldo-con-snapshot-fuera-de-la-ventana.md` |
| Reintentos con espera del listener Kafka | `DefaultErrorHandler` con backoff exponencial (5 reintentos: 1, 2, 4, 8 y 10 s); cada fallo se registra como WARN y el descarte final como ERROR con partición, offset y key (`newVersion`) | `adr/0030-reintentos-con-espera-del-listener-kafka.md` |
| Versión intermedia ausente = discontinuidad | `CatedraCatalogReadPort.fetchChanges` devuelve `Optional<CatalogChanges>`; vacío lanza `FullSyncRequiredException` en el caso de uso; JSON ilegible o ID faltante en el Hash siguen siendo `InvalidCatalogDataException` | `adr/0031-version-intermedia-ausente-como-discontinuidad.md` |
| Estado observable de la sincronización | Tabla `sync_state` de una fila en PostgreSQL (`SIN_SINCRONIZAR`, `OK`, `RECONSTRUIDO_POR_DISCONTINUIDAD`, `ERROR`); la fachada lo escribe en una transacción propia y relanza el error | `adr/0032-estado-observable-de-la-sincronizacion.md` |
| Un solo módulo hasta la entrega y desvíos conocidos | Se mantiene `catalog` hasta el 17/11; la migración a módulos se evalúa sobre el servicio funcionando; se registran los desvíos respecto de la skill | `adr/0033-un-solo-modulo-hasta-la-entrega-y-desvios-conocidos.md` |
| Disponibilidad en el filtro de búsqueda | Parámetro `availableOn` (fecha): profesionales con un horario habilitado para el día de la semana de esa fecha; es un pre-filtro, no garantiza turnos libres. Supuesto a confirmar con la cátedra el 17/11 | `adr/0034-disponibilidad-en-el-filtro-de-busqueda.md` |
| Consumidores de la búsqueda, login y filtro por nombre | KMP consume la búsqueda; turnos consume la agenda por profesional (no la búsqueda); registro y login en turnos; nombre = contiene, sin mayúsculas (las tildes no se ignoran por ahora), sobre nombre y apellido; sin paginación; endpoint de categorías | `adr/0035-consumidores-de-la-busqueda-login-y-filtro-por-nombre.md` |
| CORS | No se configura. El cliente obligatorio es una app Android nativa, que no aplica CORS (es un mecanismo de navegadores). Sin orígenes permitidos (comportamiento por defecto de Spring); no se usa `allowedOrigins("*")`. Según el profesor (comunicación verbal, pendiente de confirmar por escrito), su mención en E §9 es un error del documento; el texto de E sigue incluyéndola | — |
| Solo turnos consume el catálogo | KMP no consulta al catálogo (I §17): turnos expone la búsqueda a KMP y consulta a este servicio; el catálogo no valida tokens de usuario final. Reemplaza en parte a ADR-0035 | `adr/0036-solo-turnos-consume-el-catalogo.md` |
| Autenticación entre servicios | JWT técnico de vida corta emitido por turnos y firmado con RS256 (par de claves: turnos firma, catálogo solo verifica); scope `catalog:read`; 401/403 en problem+json; Swagger solo en desarrollo. Propuesta a confirmar | `adr/0037-jwt-tecnico-de-turnos-para-llamar-al-catalogo.md` |
| Datos inválidos de la cátedra | Se asume que llegan válidos; sin manejo especial. Si llega uno inválido, falla la unidad de trabajo y la versión local no avanza | `adr/0014-validaciones-del-modelo.md` |
| Versión 0 | No se publica (el profesor lo consideró improbable); `0` es el estado inicial, base vacía | `adr/0014-validaciones-del-modelo.md` |

## Pendientes

| Decisión | Bloquea | Notas |
|---|---|---|
| Idempotencia: deduplicar por `eventId` de Kafka (I §15.1, §18.1) | #6 | Falta decidir dónde se persiste el `eventId` procesado |
| Reintentos y reconstrucción de la sincronización (qué hacer con filas ausentes del snapshot) | #6 | Alcance de la transacción resuelto en ADR-0023; falta la política de reintentos y la limpieza previa |
| `SyncDiagnosticsPort`: tabla propia o sobre `CatalogRepositoryPort` | #7, #12 | Depende de la máquina de estados de sync |
| Efecto de una categoría deshabilitada sobre sus profesionales en la búsqueda | #9 | El significado de "nombre" quedó en ADR-0035 y el de "disponibilidad" en ADR-0034 |
