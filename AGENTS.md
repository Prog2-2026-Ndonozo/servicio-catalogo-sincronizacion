# Servicio de Catálogo y Sincronización

Backend 1 de 2 del proyecto integrador 2026 (cátedra Programación 2). Dueño exclusivo de la copia local del
catálogo (categorías, profesionales, horarios semanales) y de la versión aplicada. Sincroniza con la cátedra
(snapshot REST + Kafka/Redis incremental) y resuelve búsquedas **solo con datos locales**. Habla con
`servicio-turnos-reservas` únicamente por HTTP + JWT: nunca toca su base, ni él la nuestra.

## Fuente única de la verdad
`../documentacion/PROJECT_STATEMENT-v1.md` (**E**) y `../documentacion/INTEGRATION_REFERENCE-v2.md` (**I**).
Todo lo demás (este archivo, `docs/`) deriva de ellas. Si algo no coincide, ganan E e I. Si un dato del
contrato no está ahí, **preguntar o señalar el gap; nunca inventar** campos, estados ni endpoints.

## Cómo leer sin gastar tokens
1. **No leas E ni I completos.** Abrí `docs/fuentes.md`: mapea tema → sección → líneas. Leé solo ese rango
   (`Read` con `offset`/`limit`).
2. **No leas el skill completo.** Cargá `SKILL.md` del catálogo hexagonal solo si vas a tocar `catalog/`, y de
   `references/` solo el archivo de la tabla de abajo.
3. **Issues:** leé solo la sección del issue en curso de `docs/BacklogServicioCatalogo.md`, no el archivo entero.
4. Mantené este archivo corto (<60 líneas). Lo estable y de detalle va a `docs/`; acá solo el router.

## Qué leer según la tarea
| Tarea | Leer |
|---|---|
| Ubicar/crear una clase en `catalog/` | `docs/arquitecturaHexagonalServicioCatalogo.md` + skill `estructura-paquetes` |
| Caso de uso nuevo o cambiado | skill `casos-de-uso` |
| Adaptador REST / JPA | skill `adaptadores` |
| Adaptador Kafka / Redis / cliente HTTP | skill `adaptadores-nuevos` + `docs/fuentes.md` (I §14-15) |
| Escribir tests | skill `testing` (mappers/adapters: `testing-adaptadores`) |
| Snapshot completo / incremental / discontinuidad | `docs/fuentes.md`: E §6, I §7, §14, §15.3, §16, §18 |
| Búsqueda y filtros | E §4.1 |
| Seguridad / JWT | E §9 + `docs/DECISIONES.md` |
| Vocabulario | `docs/glosario.md` |

Skill (relativo a este repo): `../../hexagonal/.agents/skills/hexagonal-catalogo/` (`SKILL.md` + `references/`).

## Stack (no cambiar tecnologías ya propuestas)
Java 25, Spring Boot 4.1, Maven (`./mvnw`), PostgreSQL propio y exclusivo (prohibido H2/SQLite/embebidas como
persistencia principal, E §3), Kafka (consumer `catedra.catalog.{groupId}`), Redis (lectura `catedra:sync:*`;
namespace propio `alumnos:{groupId}:*` solo para estado auxiliar), springdoc, Lombok, ArchUnit.
Comandos: `./mvnw test` · `./mvnw spring-boot:run` · `docker compose up -d` (pendiente, issue #2).

## Alcance
Hace: copia local del catálogo, versión aplicada, sync completa e incremental, detección de discontinuidad,
búsqueda local, estado/errores de sync. **No** hace disponibilidad, holds ni reservas (es de `servicio-turnos-reservas`).

## Arquitectura (resumen; detalle en `docs/arquitecturaHexagonalServicioCatalogo.md`)
`catalog/{domain,application,infrastructure}` + `config/`. `domain/` sin Spring/JPA/Jakarta/Jackson.
`application/` no importa `infrastructure/`. Drivers externos entran por la fachada `@Service`, no por el puerto.
Las 7 reglas las verifica `ArchitectureTest`; `./mvnw test` debe quedar en verde. Si el usuario toma un camino
que rompe esta arquitectura, **avisar de inmediato**. Ante duda de diseño hexagonal, el skill gana.

## Reglas para el agente
- PROHIBIDO commitear, crear ramas o pushear sin confirmación previa. Cuando toque commitear, recordarle a
  Nehuen usar **Conventional Commits**.
- El JWT técnico de cátedra jamás se loguea, versiona ni expone (vive en `cuenta_tecnica.json`, gitignoreado).
- Todo cambio en la máquina de sincronización (versiones, idempotencia, reconstrucción) se registra en
  `docs/DECISIONES.md` con su justificación (formato ADR).
- Antes de tocar duplicados/reintentos/discontinuidades: E §6 y §8, I §18 y `docs/glosario.md`.
