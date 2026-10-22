# ADR-0018: Claves foráneas diferidas y reglas de esquema para la sincronización

Fecha: 2026-10-16
Estado: aceptada

## Contexto
El enunciado exige aplicar el snapshot "de forma consistente" y registrar como versión local "exactamente la
versión que corresponde a los datos almacenados" (E §6.1). En la sincronización incremental, la versión local
solo avanza cuando la unidad de trabajo terminó correctamente, el reprocesamiento no debe alterar el resultado
(E §6.2) y la aplicación debe recuperarse "sin corromper el estado local" (E §5.15). Las tablas internas y la
estrategia transaccional son decisiones a cargo del alumno (E §10).

Las tres colecciones del catálogo se referencian por ID: `Professional.categoryId` y
`WeeklySchedule.professionalId` (ADR-0010). Las bajas son lógicas (`enabled = false`, I §14.4), por lo que nunca
se borra una fila de la que otra dependa. En los cambios incrementales, el contrato publica solo los IDs
afectados y el estado vigente se lee de Redis (I §14.5): puede ser más nuevo que la versión que se está
aplicando, por ejemplo un profesional que ya apunta a una categoría creada en la versión siguiente.

Este ADR fija las reglas de esquema. La estrategia transaccional completa de la sincronización (granularidad,
reintentos, tratamiento de un dato inválido) se decide en los issues #5 y #6.

## Opciones consideradas
1. **Sin claves foráneas.** La integridad queda en el código.
   + Cualquier orden de inserción funciona; ninguna versión falla por una referencia que aún no llegó.
   − Un profesional o un horario huérfano puede quedar guardado sin que nada falle, y las búsquedas lo
     arrastran. Contradice la postura estricta del ADR-0014.
2. **Claves foráneas inmediatas.**
   + La base garantiza la integridad.
   − Exigen insertar en el orden exacto (categorías, profesionales, horarios) en cada operación.
3. **Claves foráneas `DEFERRABLE INITIALLY DEFERRED`.**
   + La base garantiza la integridad y la verifica al confirmar la transacción, así que el orden de inserción
     dentro de la unidad de trabajo no importa.
   − El error de una referencia faltante aparece al hacer commit, no en la sentencia que lo causó.

## Decisión
Opción 3. Las claves foráneas son `DEFERRABLE INITIALLY DEFERRED`:

| Tabla | Columna | Referencia |
|---|---|---|
| `professionals` | `category_id` | `professional_categories(id)` |
| `weekly_schedules` | `professional_id` | `professionals(id)` |

Reglas de esquema que acompañan a la decisión:

1. **La unidad de trabajo de la sincronización es una versión completa en una sola transacción** (el snapshot
   completo o una versión incremental). Es un principio; el detalle se decide en los issues #5 y #6.
2. **La versión local vive en la misma base de datos** que las tres colecciones, de modo que datos y versión
   se confirman o se descartan juntos.
3. **El ID asignado por la cátedra es la clave primaria** de cada tabla y no se genera localmente. Aplicar
   dos veces el mismo cambio es un *upsert* por ID y no altera el resultado (E §6.2).
4. **Se indexan las columnas de las claves foráneas** (`category_id`, `professional_id`), porque PostgreSQL no
   lo hace solo y las búsquedas por categoría y profesional las usan.

## Justificación
Las claves foráneas llevan a la base la misma postura estricta que el ADR-0014 fija en el dominio: una copia
local incoherente es peor que una sincronización que se reintenta. Diferir la verificación al commit elimina el
problema del orden dentro de la transacción y mantiene la garantía de integridad. Si una versión incremental
referencia algo que todavía no está, la transacción falla completa, la versión local no avanza y la
sincronización se reintenta, que es el comportamiento que pide E §6.2.

## Consecuencias
- Un dato huérfano **no puede** persistirse: falla toda la unidad de trabajo.
- La violación se detecta en el commit. El caso de uso (issues #5 y #6) debe tratar esa excepción como "la
  versión no se aplicó", sin avanzar la versión local; su reintento se define en esos issues.
- Las migraciones (ADR-0017) declaran las claves con `DEFERRABLE INITIALLY DEFERRED`; Hibernate en
  `ddl-auto: validate` no las verifica, así que el esquema lo garantiza Flyway.
- Los tests del adaptador de persistencia necesitan una base PostgreSQL real: el diferimiento de
  restricciones no existe en H2 de la misma forma, y H2 está prohibido de todos modos (E §3).
- No cierra la pregunta abierta del ADR-0010 sobre el efecto de una categoría deshabilitada en sus
  profesionales: una categoría con `enabled = false` sigue existiendo, así que la clave foránea no se rompe.
