# Decisiones de diseño

Registro de decisiones (E §10 y §12). Cada decisión importante tiene su ADR en `docs/adr/NNNN-titulo.md`
(plantilla: `../../ADR-template.md`). Esta tabla es el índice: se lee primero, el ADR solo si hace falta el detalle.

## Tomadas

| Decisión | Resumen | Dónde |
|---|---|---|
| Arquitectura hexagonal, un solo módulo `catalog` | Tres capas + `config/`; dominio sin framework; reglas verificadas con ArchUnit | `arquitecturaHexagonalServicioCatalogo.md` |
| Motor de base de datos | PostgreSQL propio y exclusivo (E §3) | — |
| Estructura de capas | Fijada en issue #1; los fixtures de `arquitecturafixtures/` prueban que las reglas detectan violaciones | `arquitecturaHexagonalServicioCatalogo.md` |

## Pendientes

| Decisión | Bloquea | Notas |
|---|---|---|
| Idempotencia: deduplicar por `eventId` de Kafka (I §15.1, §18.1) | #6 | Falta decidir dónde se persiste el `eventId` procesado |
| Qué significa "disponibilidad" en el filtro de búsqueda | #9 (issue #8, ADR) | E §4.1 no lo define |
| Quién consume la búsqueda: ¿KMP directo + turnos, o solo turnos? | #9 | Si solo turnos, se elimina la fila de KMP de la tabla de puertos de entrada |
| Dónde viven registro/login del usuario final (E §3.2) | — | E no asigna el servicio; `reunion.md` lo ubica en catálogo. No está en el backlog |
| Propagación de identidad entre servicios (JWT usuario vs técnico) | #10 | `ADR-0002` aún no escrito (E §9) |
| `SyncDiagnosticsPort`: tabla propia o sobre `CatalogRepositoryPort` | #7, #12 | Depende de la máquina de estados de sync |
