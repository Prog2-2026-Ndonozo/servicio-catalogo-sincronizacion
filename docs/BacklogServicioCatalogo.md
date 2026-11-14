# Backlog — servicio de catálogo y sincronización

> Notación: `E §x` = `PROJECT_STATEMENT`, `I §x` = `INTEGRATION_REFERENCE` (ver `fuentes.md`).

> v2: un issue = un caso de uso/funcionalidad = un commit o PR coherente. El desglose por capa hexagonal (puerto/adaptador/dominio/test) vive como checklist *dentro* de cada issue, no como issues separados — así el historial de commits queda legible por funcionalidad, tal como pide E §13.1.

## Labels sugeridos

`feature` · `chore` · `docs` · `security` · `decision-needed` (bloquea el feature hasta resolverse con un ADR)

## Convención de trabajo

- Un issue = una rama (`feature/5-sincronizacion-completa`) = un PR que lo cierra (`Closes #5`).
- El checklist de cada issue es la guía interna de implementación (capas hexagonales), no son sub-issues.
- Un PR no se mergea si el sistema queda en un estado a medio implementar — el caso de uso tiene que funcionar de punta a punta (aunque sea con lo mínimo) antes de cerrar el issue.
- DoD por issue, salvo que diga lo contrario:
  - [ ] Todos los ítems del checklist interno resueltos
  - [ ] Código en la capa correcta según `docs/arquitecturaHexagonalServicioCatalogo.md`
  - [ ] Al menos un test que cubra el caso de uso
  - [ ] `./mvnw test` en verde (incluye `ArchitectureTest`, que no se puede esquivar)
  - [ ] Sin secretos ni credenciales hardcodeadas
  - [ ] docs/ actualizado si el issue cambia un contrato externo

---

## Milestone 0 — Bootstrap

No son casos de uso funcionales, son la plomería previa — cada uno ya es un commit natural propio (`chore:`).

### #1 chore: setup del proyecto + esqueleto hexagonal `[chore]`
Proyecto Spring Boot inicial + estructura de capas según `docs/arquitecturaHexagonalServicioCatalogo`: `catalog/{domain,application,infrastructure}` y `config/`, vacías. Las siete reglas de dependencia van en `ArchitectureRules`/`ArchitectureTest` (ArchUnit), y `ArchitectureRulesFixtureTest` comprueba con clases de prueba que cada regla detecta una violación real — si no, el test verde no probaría nada. No requiere ninguna clase de dominio: la guarda se verifica con fixtures.

### #2 chore: Docker Compose local `[chore]`
PostgreSQL propio + variables de entorno externalizadas (`application.yml` con placeholders, `.env.example`).

### #3 feat: modelo de dominio base `[feature]`
`Professional`, `ProfessionalCategory`, `WeeklySchedule`, `CatalogVersion` como clases de dominio puras, con las validaciones mínimas del contrato del snapshot.

### #4 chore: puerto de persistencia + esquema inicial `[chore]`
`CatalogRepositoryPort` + `JpaCatalogRepositoryAdapter` con migraciones (Flyway/Liquibase). Solo esquema, sin lógica de negocio todavía.

---

## Milestone 1 — Sincronización completa

Cubre evidencia mínima: *"una inicialización desde snapshot"* (E §11).

### #5 feat: sincronización completa del catálogo desde snapshot `[feature]`
- [x] Puerto `CatedraSnapshotPort` + adaptador REST (`GET /api/synchronization/snapshot`, JWT técnico)
- [x] Caso de uso `ApplyFullSnapshotUseCase`: aplica versión + 3 colecciones de forma transaccional (E §6.1)
- [x] Disparo de la sincronización completa (al bootear con base vacía, o endpoint interno)
- [x] Test: si falla a mitad de camino, la versión local no avanza

---

## Milestone 2 — Sincronización incremental

Cubre evidencia mínima: *"una actualización incremental del catálogo"* + *"procesamiento idempotente de un mensaje duplicado"*.

### #6 feat: sincronización incremental de catálogo vía Kafka + Redis `[feature]`
> Desglosado en los issues de GitHub #12 a #16 (la numeración de GitHub no coincide con la de este backlog).

- [x] Adaptador Kafka de entrada: listener de `CatalogUpdated` en `catedra.catalog.{groupId}` — GitHub #12 (ADR-0026, ADR-0030)
- [x] Puerto `CatedraCatalogReadPort` + adaptador Redis (metadata, Hashes, `changes:{version}`) — GitHub #13 (ADR-0027)
- [x] Caso de uso `ApplyIncrementalChangeUseCase`: aplica en orden, avanza versión solo tras éxito total (E §6.2) — GitHub #14
- [x] Deduplicación por `eventId` (evento repetido no reaplica efectos) — GitHub #15 (ADR-0028)
- [x] Manejo de versión fuera de orden (no aplicar fuera de secuencia) — GitHub #16 (recorre `local+1..newVersion`; ADR-0029)
- [x] Test: mismo evento procesado dos veces produce el mismo estado final que procesarlo una vez

---

## Milestone 3 — Discontinuidad y reconstrucción

Cubre evidencia mínima: *"recuperación mediante snapshot luego de una discontinuidad"*.

### #7 feat: detección de discontinuidad y reconstrucción automática `[feature]`
- [ ] Chequeo de versión local vs `oldestAvailableVersion` antes de intentar incremental
- [ ] Si está fuera de ventana o falta una versión intermedia → dispara `ApplyFullSnapshotUseCase` (reutiliza #5)
- [ ] Estado observable de sincronización (`OK`, `RECONSTRUIDO_POR_DISCONTINUIDAD`, `ERROR`) — base del Milestone 5
- [ ] Test: reproducir el escenario exacto de I §18.2 (local=3, current=7, oldest=4 → snapshot, nunca `changes:4`)

---

## Milestone 4 — Búsqueda y filtros

Cubre evidencia mínima: *"búsquedas resueltas sobre datos locales"* + *"funcionamiento de los filtros obligatorios"*.

### #8 decision: criterio de "disponibilidad" para el filtro `[decision-needed]`
No es código — es resolver con un ADR qué significa exactamente "disponibilidad" (¿algún horario habilitado? ¿en un rango de fechas?) antes de que el #9 lo implemente. Bloquea a #9.

**Resuelto:** ADR-0034 (`availableOn`: atiende ese día de la semana; supuesto a confirmar con la cátedra el 17/11).

### #9 feat: búsqueda de profesionales con filtros obligatorios `[feature]`
- [ ] Caso de uso `SearchProfessionalsUseCase`: categoría, nombre, estado habilitado, disponibilidad (según #8)
- [ ] Adaptador REST, consumido solo por servicio-turnos (ADR-0036); la protección con JWT entre servicios es el #10
- [ ] Filtro por nombre: contiene, sin mayúsculas, sobre nombre y apellido; las tildes no se ignoran por ahora (ADR-0035)
- [ ] Endpoint de lectura de categorías, para que servicio-turnos ofrezca a KMP el filtro por categoría (ADR-0035, ADR-0036)
- [ ] Sin paginación por ahora (ADR-0035)
- [ ] Test: filtros combinados y verificación de que nunca se llama a la cátedra por búsqueda

---

## Milestone 5 — Seguridad y robustez inter-servicios

Cada uno es una funcionalidad transversal autocontenida — issue propio, commit propio.

> ⚠️ **Cambio de alcance (2026-11-13, ADR-0036):** la interfaz KMP **no consulta a este servicio**. El único cliente HTTP es
> `servicio-turnos`. Por eso este servicio **no valida JWT de usuario final** (el login vive en turnos), solo el token con que
> turnos se identifica; los endpoints de búsqueda y categorías pasan a ser contratos entre servicios; y CORS no tiene destinatario.

### #10 feat: JWT inter-servicios para servicio-turnos `[security]`
Según la decisión ya tomada en `ADR-0002` (propagación de identidad).

- [x] Validar el JWT de servicio-turnos (firma, vigencia y permisos, E §9) en **todos** los endpoints de datos: búsqueda, categorías, agenda (el estado llega con el #12). El catálogo no recibe tokens de usuario final (ADR-0036)
- [x] Decidir y justificar la autenticación entre servicios (E §9): JWT técnico de turnos firmado con RS256 — ADR-0037
- [x] Configurar el servidor de recursos: clave pública, emisor, audiencia y `SecurityFilterChain` (stateless, CSRF desactivado, `SCOPE_catalog:read`)
- [x] Respuestas 401 y 403 como `application/problem+json` con `code` estable
- [x] Swagger solo en el perfil de desarrollo, con esquema bearer para "Authorize"
- [x] Tests: token válido, sin token, firma inválida, vencido, otro emisor, otra audiencia y sin permiso (con decodificador real)
- [x] Documentar el contrato del token para el servicio de turnos — `docs/contratos/catalogo-para-turnos.md`
- [x] Endpoint de agenda vigente de un profesional para servicio-turnos (E §4.2, E §7, I §17; ADR-0035): profesional, su estado y sus horarios semanales con `enabled`

### #11 feat: resiliencia ante indisponibilidad de cátedra `[feature]`
Timeouts + reintentos acotados al llamar REST/Redis/Kafka de cátedra. Las búsquedas siguen funcionando con la última copia local (E §8).

### #12 feat: endpoint de estado y errores de sincronización `[feature]`
Expone el estado guardado en #7 (E §4.1: *"informar el estado y los errores de sincronización"*). Consumidor: servicio-turnos y diagnóstico propio, no KMP (ADR-0036).

### #13 feat: CORS y validación de entrada `[security]`
Validación de DTOs en cada endpoint (E §9). **CORS no se implementa**: ningún navegador ni app llega a este servicio (solo turnos, ADR-0036) y,
según el profesor (verbal, a confirmar por escrito), su mención en E §9 es un error del documento. Ver `DECISIONES.md`.

---

## Milestone 6 — Documentación y cierre

### #14 docs: README reproducible `[docs]`
### #15 docs: diagrama de arquitectura hexagonal del servicio `[docs]`
### #16 docs: contrato REST expuesto a servicio-turnos (DTOs, auth, errores) `[docs]`
### #17 docs: checklist final de evidencias mínimas de catálogo (E §11) `[docs]`