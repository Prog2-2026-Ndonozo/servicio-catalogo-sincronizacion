# ADR-0013: Tipos Java para identificadores, duración, versión, textos y estado

Fecha: 2026-10-06
Estado: aceptada

## Contexto
Los identificadores (`id`, `categoryId`, `professionalId`) los asigna la cátedra y el contrato los define como
enteros de 64 bits (I §8, `int64`). La duración del turno (`slotDurationMinutes`, I §7) es un entero chico y
obligatorio: sin ella no se pueden calcular los slots.

## Opciones consideradas
1. **IDs como `Long`** — cubre todo el rango de 64 bits del contrato.
2. **IDs como `Integer`** — más chico, pero un ID grande de la cátedra no cabría y fallaría.
3. **Duración como `int`** — nunca puede ser nulo: el propio tipo impide que falte.
4. **Duración como `Integer`** — admite nulo, lo que obliga a validar su ausencia.

## Decisión
- `id`, `categoryId`, `professionalId`: `Long`.
- `slotDurationMinutes`: `int`.
- Versión de catálogo (local aplicada): `long`. Es un número que viene de la cátedra, igual que los IDs.
- `name`, `description`, `firstName`, `lastName`: `String`.
- `enabled`: `boolean`. No admite nulo, igual que `int`: es un dato obligatorio.

## Justificación
Los IDs los define la cátedra, así que nuestro tipo debe poder representar cualquier valor del contrato; cambiar el
tipo después, con la base ya creada, costaría más que usar `Long` desde el principio. La duración es obligatoria
y de rango acotado, por lo que `int` expresa mejor el dato y evita un estado "sin duración".

Los tipos de fecha y hora están en ADR-0012; las reglas de validez en ADR-0014.

## Consecuencias
- Las columnas de ID en PostgreSQL deben ser de 64 bits (`bigint`); se define en el issue #4.
- Los mappers convierten entre estos tipos y los del JSON y de JPA.
- Que `slotDurationMinutes` sea mayor que 0 no lo garantiza el tipo: es una validación que se decide aparte.
