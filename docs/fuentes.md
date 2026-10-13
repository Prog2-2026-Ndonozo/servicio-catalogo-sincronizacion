# Fuentes de verdad y mapa de lectura

Dos documentos mandan sobre todo lo demás, y viven fuera de este repo:

| Sigla | Archivo | Qué es |
|---|---|---|
| **E** | `../documentacion/PROJECT_STATEMENT-v1.md` | Enunciado: qué hay que construir, restricciones, evidencias, entrega |
| **I** | `../documentacion/INTEGRATION_REFERENCE-v2.md` | Contratos REST / Redis / Kafka con la cátedra y recuperación |

Si `docs/` o el código contradicen a E o I, gana E o I. Las citas del repo usan esta notación: `E §6.1`, `I §18.2`.

> Los números de línea son de las versiones indicadas (E v1, I v2). Si los archivos cambian, relocalizar con
> `grep -n '^## \|^### ' <archivo>` y actualizar esta tabla.

## Qué leer de I para este servicio

| Tema | Sección | Líneas |
|---|---|---|
| `groupId`, JWT técnico vs usuario final | I §2 | 11-26 |
| Convenciones (fechas, JSON, `problem+json`, Kafka at-least-once) | I §4 | 40-51 |
| Config de integración (hosts, topics, namespaces) | I §5.1-5.3 | 55-173 |
| Mapa REST | I §6 | 179-193 |
| **Snapshot** `GET /api/synchronization/snapshot` | I §7 | 195-249 |
| Errores REST (formato; matriz solo si el adapter los traduce) | I §13 | 438-509 |
| **Redis**: permisos, versiones, metadata, entidades, `changes:{v}`, namespace propio | I §14 | 511-627 |
| **Kafka**: convenciones y topics | I §15.1-15.2 | 631-650 |
| **Kafka** `CatalogUpdated` | I §15.3 | 652-667 |
| Diagrama de sincronización | I §16 | 820-847 |
| Recuperación: duplicado, versión perdida, reintentos | I §18.1, 18.2, 18.4 | 896-906, 914-916 |
| Límite del contrato (qué decide el alumno) | I §19 | 918-930 |

**No leer** en este repo (son de `servicio-turnos-reservas`): I §8-12 (ocupaciones, holds, reservas),
I §15.4-15.10 (eventos de turnos), I §17 (diagrama de reserva), I §18.3.

## Qué leer de E para este servicio

| Tema | Sección | Líneas |
|---|---|---|
| Separación entre servicios, BD propia, Docker Compose | E §3 | 38-60 |
| Usuarios finales y JWT (`login`, campos de registro) | E §3.2 | 68-84 |
| Responsabilidades de catálogo | E §4.1 | 88-101 |
| Alcance funcional (ítems 4-7, 15) | E §5 | 120-138 |
| **Sincronización completa e incremental** | E §6 | 140-156 |
| Robustez y recuperación | E §8 | 180-195 |
| Seguridad (checklist completo) | E §9 | 197-220 |
| Decisiones a cargo del alumno | E §10 | 222-244 |
| Pruebas mínimas | E §10.1 | 240-244 |
| Evidencias mínimas (las de catálogo) | E §11 | 246-265 |
| Documentación obligatoria | E §12 | 267-280 |
| Tres repos Git, historial, entrega | E §13.1 | 284-296 |

## Mapa puerto → contrato

Para implementar un puerto, leer solo su sección de I:

| Puerto / adaptador | Contrato |
|---|---|
| `CatedraSnapshotPort` → `catedrarest` | I §7 (+ §4 JWT, §13 errores) |
| `CatedraCatalogReadPort` → `catedraredis` | I §14.2-14.5 |
| `CatalogUpdatedListener` → `messaging` | I §15.1-15.3 (`eventId` = clave de idempotencia) |
| `ApplyIncrementalChangeUseCase` | E §6.2, I §14.5, §16, §18.1-18.2 |
| `ApplyFullSnapshotUseCase` | E §6.1, I §7, §18.2 |
| `SyncDiagnosticsPort` | I §14.6 (namespace propio, opcional) |
| `SearchProfessionalsUseCase` | E §4.1 (filtros: categoría, nombre, habilitado, disponibilidad) |

## Otros documentos del repo

| Archivo | Contenido |
|---|---|
| `docs/arquitecturaHexagonalServicioCatalogo.md` | Aplicación del skill hexagonal a este servicio |
| `docs/BacklogServicioCatalogo.md` | Issues, milestones y DoD |
| `docs/DECISIONES.md` | Decisiones tomadas y pendientes (ADR) |
| `docs/glosario.md` | Vocabulario del proyecto |
