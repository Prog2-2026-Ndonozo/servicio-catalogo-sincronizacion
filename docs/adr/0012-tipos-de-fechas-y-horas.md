# ADR-0012: Tipos Java para fechas, horas y día de la semana

Fecha: 2026-10-06
Estado: aceptada

## Contexto
El contrato publica tres clases de valores temporales (I §4, I §7, I §14.4): instantes en ISO-8601 UTC
(`createdAt`, `updatedAt`), horas locales de atención (`startTime`, `endTime`, con formato `HH:mm` o `HH:mm:ss`)
y el día de la semana (`dayOfWeek`: `MONDAY` a `SUNDAY`). El dominio debe usar tipos propios de Java sin
depender de librerías externas (`DOMINIO_ES_PURO`).

## Opciones consideradas
1. **Tipos de `java.time` ajustados a cada significado** (`Instant`, `LocalTime`, `DayOfWeek`).
   + Cada tipo expresa exactamente el dato; validan formato y rango; están en el JDK.
2. **`LocalDateTime` / `Date` / `String`.**
   − `LocalDateTime` no tiene zona y vuelve ambiguo un instante UTC; `Date` es la API heredada;
     `String` no valida ni se compara correctamente.

## Decisión
Opción 1:

| Dato | Tipo Java |
|---|---|
| `createdAt`, `updatedAt` | `java.time.Instant` |
| `startTime`, `endTime` | `java.time.LocalTime` |
| `dayOfWeek` | `java.time.DayOfWeek` |

## Justificación
`Instant` representa un punto único en el tiempo y se corresponde con el UTC del contrato. `LocalTime` es una
hora del día sin fecha ni zona, que es lo que expresa un horario semanal ("atiende a las 9"). `DayOfWeek`
tiene exactamente los siete valores del contrato.

## Consecuencias
- Que el contrato acepte `HH:mm` y `HH:mm:ss` (I §4) es un asunto de lectura del JSON: lo resuelve el adaptador,
  el dominio solo guarda un `LocalTime`.
- Los mappers de `infrastructure/` convierten entre estos tipos y el JSON de la cátedra y los tipos de JPA.
- Las validaciones que comparan horas (por ejemplo `startTime` anterior a `endTime`) se deciden aparte.
