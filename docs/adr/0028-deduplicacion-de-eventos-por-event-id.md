# ADR-0028: Deduplicación de eventos `CatalogUpdated` por `eventId` en PostgreSQL

Fecha: 2026-11-06
Estado: propuesta

## Contexto
Kafka entrega los eventos **al menos una vez**: el mismo `CatalogUpdated` puede llegar más de una vez, y con
`auto-offset-reset: earliest` (ADR-0026) un primer arranque relee todo el historial retenido. El contrato indica
que el `eventId` es la clave de idempotencia y que los consumidores deben tolerar repeticiones (I §15.2-15.3).
El enunciado exige que el reprocesamiento no altere el resultado y que la versión local solo avance cuando la
unidad de trabajo terminó bien (E §6.2, E §5.15).

El guardado de entidades ya es idempotente (insertar o actualizar por ID, ADR-0018): aplicar dos veces el mismo
cambio deja el mismo estado. La deduplicación, entonces, no protege la integridad de los datos sino que cumple el
contrato, evita lecturas repetidas a Redis y deja registro de lo procesado.

Hace falta decidir dónde se guardan los `eventId` ya procesados.

## Opciones consideradas
1. **Tabla en PostgreSQL** (migración Flyway), escrita en la misma transacción que aplica el cambio.
   + Atómica con la aplicación del cambio y el avance de la versión: o ocurre todo o nada.
   + Es nuestra base; no depende de permisos ni de la disponibilidad de un sistema externo.
   − Una migración y un puerto/adaptador más; la tabla crece y requiere limpieza.
2. **Claves en Redis**, en el espacio `alumnos:{groupId}:*` (I §14.1).
   + Sin migración; admite TTL.
   − No es atómico con PostgreSQL: marcar antes de aplicar puede perder un evento si el servicio cae; marcar
     después puede aplicarlo dos veces.
   − Es el Redis compartido de la cátedra: no controlamos reinicios y requiere permiso de escritura.
3. **Solo comparar versiones**, sin guardar `eventId`.
   + Nada que persistir.
   − No cumple la indicación del contrato y no distingue un evento repetido de uno distinto con la misma versión.

## Decisión
Opción 1. Una tabla `processed_events` en PostgreSQL:

| Columna | Tipo | Detalle |
|---|---|---|
| `event_id` | `TEXT` | Clave primaria; no vacío (`btrim(event_id) <> ''`) |
| `new_version` | `BIGINT` | `> 0` |
| `processed_at` | `TIMESTAMPTZ` | Momento en que se aplicó |

**Flujo del caso de uso incremental**, dentro de una única transacción (`@Transactional`, como ADR-0023):
1. Si `newVersion <=` versión local, el evento ya está aplicado: se ignora sin consultar la tabla ni Redis.
2. Si el `eventId` ya figura en `processed_events`, se ignora.
3. En otro caso se aplican los cambios, se avanza la versión local y se inserta el `eventId`.

Si cualquier paso falla, la transacción se revierte: ni la versión ni el `eventId` quedan registrados y el
reintento de Spring Kafka (ADR-0026) vuelve a intentar con el estado anterior intacto.

Se agrega un puerto de salida de dominio (nombre a definir en la implementación) para consultar y registrar
eventos procesados, con un adaptador JPA; el dominio sigue sin depender de Spring.

## Consecuencias
- Nueva migración `V2__crear_processed_events.sql`. Una migración aplicada no se edita (README).
- El control de versión (paso 1) absorbe casi todas las repeticiones; el `eventId` es la segunda barrera.
- Dos eventos con el mismo `eventId` y distinta versión no deberían existir; si ocurriera, se aplica el primero
  y el segundo se ignora por `eventId`.
- **Limpieza pendiente:** la tabla crece con cada evento. Podría borrarse lo anterior a la
  `oldestAvailableVersion` de la ventana de la cátedra, pero no se decide acá; mientras tanto el volumen es bajo.
- La versión fuera de orden (evento con `newVersion` mayor al siguiente a la local) se resuelve en su propio
  issue y se apoya en esta tabla y en `changes:{v}` (ADR-0027).
- Tests: evento nuevo se aplica y queda registrado; evento repetido no modifica nada; falla a mitad de la
  aplicación no deja el `eventId` registrado (reversión).
