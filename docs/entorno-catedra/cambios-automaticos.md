# Cambios automáticos de la cátedra

Información del entorno de pruebas; no es parte del contrato (E / I). Si algo no coincide, ganan E e I.

## Qué hace la cátedra
Desde el **2026-11-04** corre un proceso automático que habilita o deshabilita un profesional (`enabled`). Cada
cambio sube la versión del catálogo y publica un `CatalogUpdated` en Kafka (topic `catedra.catalog.{groupId}`).
Antes de esa fecha el topic estaba vacío. Sirve para probar la sincronización incremental con datos reales.

## Qué se observó
- Snapshot inicial local: versión 1.
- El listener recibió eventos con `newVersion` 2, 3 y 4.
- Con el caso de uso incremental en stub (`UnsupportedOperationException`), cada evento se reintentó 10 veces
  (`DefaultErrorHandler`, sin espera entre intentos) y luego se saltó.

## Cómo probar
1. Dejar el servicio corriendo con la base ya sincronizada (versión local mayor a 0).
2. Esperar un evento; el log muestra `CatalogUpdated recibido (eventId=..., newVersion=...)`.
3. Con el caso de uso real: verificar que el profesional cambie su `enabled` en la base local y que la versión
   local avance a `newVersion`.

## Cuidados
- Los offsets del grupo `alumnos-{groupId}` ya avanzaron: los eventos 2-4 no se vuelven a recibir con el mismo
  `group-id`. Para releerlos hay que usar otro `group-id` (el `auto-offset-reset: earliest` solo vale para un grupo
  sin offset guardado, ADR-0026).
- Como esos eventos se saltaron, la versión local puede quedar atrás: el caso de uso incremental tiene que
  ponerse al día con `catedra:sync:changes:{v}` cuando llegue un evento posterior (issue de versión fuera de orden).
- Un cambio de `enabled` es una baja lógica: la entidad sigue en los Hashes de Redis (I §14.4).
