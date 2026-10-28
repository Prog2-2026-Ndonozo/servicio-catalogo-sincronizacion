# ADR-0022: Fallos de la cátedra traducidos a una sola excepción del dominio

Fecha: 2026-10-28
Estado: aceptada

## Contexto
La sincronización completa pide el snapshot con `GET /api/synchronization/snapshot` (I §7). La llamada puede
fallar de varias formas: no hay red o vence el tiempo, la cátedra responde `401`/`403` (JWT vencido o inválido),
`404` (URL mal configurada), `5xx` (problema del lado de la cátedra), o responde `200` con un cuerpo vacío o
ilegible. Según I §13 los clientes deben decidir por `status` y, cuando exista, por `code`; los rechazos `401` y
`403` de Spring Security y los `500`/`503` pueden no traer `code`.

Ante cualquiera de estos fallos, el caso de uso reacciona igual: no hay snapshot, no se aplica nada y la versión
local no avanza (ADR-0018, E §6.1). La política de reintentos todavía no existe (issue #6, I §18.4). El puerto
`CatedraSnapshotPort` está en `domain/`, que no puede depender de `application/` ni de ningún tipo HTTP.

## Opciones consideradas
1. **Una excepción con datos.** `CatedraUnavailableException` con mensaje, causa y el status HTTP (si hubo).
   + Simple; el caso de uso no se acopla a los códigos HTTP; el log y el diagnóstico conservan la información.
   + Se puede dividir más adelante sin cambiar a quienes la capturan.
2. **Dos tipos, reintentable y no reintentable.**
   − Útil cuando exista el reintento; hoy no hay política que lo use.
3. **Una excepción por status** (`Unauthorized…`, `ServerError…`, etc.).
   − Mucho código y acopla el núcleo a los códigos de la cátedra.

## Decisión
Opción 1. `CatedraUnavailableException` vive en `catalog/domain/exception`, junto a `InvalidCatalogDataException`
(mismo razonamiento del ADR-0016: el puerto está en el dominio). Tiene tres formas de construirse: solo mensaje,
mensaje y status HTTP, y mensaje y causa.

`CatedraRestClient` la lanza en estos casos:
- Cualquier respuesta `4xx` o `5xx`, con ese status (`httpStatus`).
- Un `200` con cuerpo vacío, sin status.
- Un fallo sin respuesta (red, tiempo vencido, JSON ilegible), sin status y conservando la causa original.

El adaptador `CatedraSnapshotRestAdapter` no captura nada: la excepción del cliente y la
`InvalidCatalogDataException` del mapper llegan intactas al caso de uso.

## Justificación
Para decidir qué hacer, el caso de uso solo necesita saber que no pudo obtener un snapshot utilizable. Para
informar y diagnosticar sí hace falta saber por qué, y eso lo cubre el `httpStatus` y la causa. Separar tipos
antes de tener una política de reintentos sería diseñar sin necesidad. El snapshot es una lectura sin efectos, por
lo que repetir la llamada es seguro cuando el reintento exista (I §18.4).

## Consecuencias
- El caso de uso (issue #5) captura `CatedraUnavailableException` y `InvalidCatalogDataException` como "no se pudo
  aplicar" y no avanza la versión local.
- Los mensajes de la excepción no incluyen el header `Authorization` ni el JWT (ADR-0020); los tests lo verifican.
- Si el issue #6 necesita distinguir fallos transitorios de permanentes, se parte la excepción usando el
  `httpStatus` ya disponible.
- `httpStatus` es un `Integer` sin tipos de Spring, así que el dominio sigue libre de framework.
