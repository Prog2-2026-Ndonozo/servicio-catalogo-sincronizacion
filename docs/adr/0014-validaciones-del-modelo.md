# ADR-0014: Validaciones de los conceptos del modelo de dominio

Fecha: 2026-10-06
Estado: aceptada

## Contexto
El issue #3 pide "las validaciones mínimas" del modelo. El contrato (I §7, I §14.4) define la forma de los datos
pero no la mayoría de las reglas de validez (largos, rangos, coherencia entre campos). Las reglas de este ADR son
decisiones del servicio; salvo indicación, **no** provienen del contrato.

## Opciones consideradas
Para cada regla se evaluó ser estricto (rechazar el dato) o permisivo (aceptarlo tal cual lo publica la cátedra).
La estricta mantiene el dominio coherente; la permisiva evita que un dato inesperado de la cátedra bloquee la
sincronización. Se eligió estricta en los casos de abajo.

## Decisión

| Concepto | Dato | Regla |
|---|---|---|
| Los tres conceptos con entidad | `id`, `createdAt`, `updatedAt` | Obligatorios: no nulos |
| `Professional` | `categoryId` | Obligatorio: no nulo |
| `WeeklySchedule` | `professionalId`, `dayOfWeek`, `startTime`, `endTime` | Obligatorios: no nulos |
| `Professional` | `firstName`, `lastName` | No nulos; no vacíos ni solo espacios |
| `ProfessionalCategory` | `name` | No nulo; no vacío ni solo espacios |
| `ProfessionalCategory` | `description` | Nunca nula; `""` permitido. El adaptador convierte `null` en `""` |
| `WeeklySchedule` | `startTime`, `endTime` | `startTime` estrictamente anterior a `endTime` |
| `WeeklySchedule` | `slotDurationMinutes` | Mayor que 0, y `endTime - startTime` (en minutos) es múltiplo exacto de la duración |
| `CatalogVersion` | versión | Nunca negativa; `0` es el estado inicial (base vacía, sin snapshot aplicado) |

Descartado: un mínimo de 30 minutos por turno, por no figurar en E ni en I.

Los datos de tipo primitivo (`boolean enabled`, `int slotDurationMinutes`, `long` de la versión) no admiten nulo por
su propio tipo (ADR-0013). El resto de los datos, que sí admitirían nulo, son todos obligatorios, salvo
`description` (nunca nula, pero `""` es válido).

## Justificación
Un profesional o una categoría sin nombre, un horario que termina antes de empezar o un turno que no divide
al horario no representan datos utilizables para buscar ni para construir disponibilidad. Con `0` como versión
inicial, el chequeo contra `oldestAvailableVersion` (I §14.2) dispara el snapshot sin un caso especial para
"base vacía" (E §6.1).

## Consecuencias
- **Más estricto que el contrato.** Si la cátedra publica un dato que viole alguna regla, o que omita un dato
  obligatorio, el dominio lo rechaza. **Decisión (conversada con el profesor):** se asume que la cátedra publica
  datos válidos y no se diseña un manejo especial para el caso contrario. Si llegara uno inválido, el modelo lanza
  `InvalidCatalogDataException`, la transacción de la sincronización se revierte y la versión local no avanza (es
  el comportamiento por defecto, sin código adicional). Las validaciones del modelo se mantienen como red de
  seguridad.
- Un horario que cruza la medianoche (por ejemplo 22:00 a 02:00) es inválido.
- Un `startTime`/`endTime` con segundos que no den un rango en minutos exactos no cumple la regla del múltiplo.
- Supuesto vigente: la versión `0` no se publica como versión real del catálogo. El profesor lo consideró
  improbable, por lo que `0` se mantiene como estado inicial (base vacía, sin snapshot aplicado).
- Los tests unitarios del modelo (issue #3) cubren cada regla sin Spring.
