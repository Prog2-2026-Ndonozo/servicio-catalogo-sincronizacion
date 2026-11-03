# ADR-0026: Listener Kafka de `CatalogUpdated`

Fecha: 2026-11-03
Estado: aceptada

## Contexto
La cátedra publica un evento `CatalogUpdated` en el topic `catedra.catalog.{groupId}` cada vez que cambia el
catálogo (I §15). La entrega es al menos una vez, el `eventId` es la clave de idempotencia y los consumidores deben
ignorar campos desconocidos (I §15.1-15.3). El evento solo **notifica** (`eventId`, `eventType`, `newVersion`,
`occurredAt`, `source`, `schemaVersion`); los datos del cambio se leen de Redis (I §14). Hace falta un adaptador
de entrada que reciba el evento y lo entregue a la aplicación sin que un mensaje defectuoso bloquee la partición.

## Opciones consideradas
1. **Recibir el payload como `String` y parsearlo nosotros** (`JsonMapper` + `CatalogUpdatedMapper`).
   + Un mensaje ilegible no falla en el deserializador de Kafka; lo vemos y decidimos qué hacer.
   − Un paso de parseo propio.
2. **Usar `JsonDeserializer` de Spring Kafka** con un DTO.
   + Menos código.
   − Un JSON inválido falla antes de entrar al método y, mal configurado, se reintenta para siempre (*poison pill*).
3. **Atrapar toda excepción en el listener.**
   − Un fallo operativo (base caída) se descartaría en silencio y se perdería el evento.

## Decisión
Opción 1. `CatalogUpdatedListener` (infraestructura, `@KafkaListener` sobre `catedra.catalog-topic`) recibe un
`String` y hace dos pasos: leer el JSON a `CatalogUpdatedMessage` y traducirlo con `CatalogUpdatedMapper` a
`CatalogUpdate` (dominio). Después llama a la fachada `CatalogSyncService.applyCatalogUpdate` (los drivers entran
por la fachada, no por el puerto).

**Política de errores:**
- **Mensaje defectuoso** (JSON ilegible, `eventType` distinto de `CatalogUpdated`, `schemaVersion` distinto de 1,
  `eventId` vacío, `newVersion` no positivo, payload `null`): se registra un `log.warn` y se **descarta**. Reintentar
  no lo arregla. Lo cubren `JacksonException` e `InvalidCatalogDataException`; el mapper rechaza el `null`.
- **Fallo de la aplicación o de infraestructura**: la excepción **se propaga** y rige el manejo por defecto de
  Spring Kafka: reintentos limitados y, agotados, se registra y se salta el mensaje (I §18.4).

**Consumidor:** `group-id` = `CATEDRA_KAFKA_CONSUMER_GROUP` (`alumnos-{groupId}`, distinto del `groupId` de I §2.1)
y `auto-offset-reset: earliest`. Tópico, broker y grupo vienen del `.env`.

**Por qué `earliest`:** el valor solo se usa cuando el grupo no tiene offset guardado (primer arranque). Con
`latest` se perderían los avisos publicados entre la descarga del snapshot y la conexión del listener, o mientras
el servicio estuvo apagado. Con `earliest` se leen y el servicio se pone al día. El costo es releer el historial
retenido; eso es seguro porque los eventos repetidos o viejos se ignoran por `eventId` y por versión
(issues de deduplicación y de versión fuera de orden).

## Consecuencias
- Si el broker no está disponible al arrancar, la aplicación **igual arranca** y el cliente reintenta la conexión,
  en la línea de ADR-0025. No hay propiedad para apagar el listener (pendiente si hace falta en tests).
- Mientras el caso de uso incremental sea un stub, cada evento termina en error con reintentos y un registro en
  el log. Es esperado hasta completar el issue #6.
- La seguridad de `earliest` depende de la deduplicación por `eventId` y del control de versión; hasta que existan,
  un primer arranque con historial largo produce ruido en el log.
- Cambiar `auto-offset-reset` no afecta a un grupo que ya tiene offset: habría que usar otro `group-id` o reiniciar
  los offsets del grupo en el broker.
- Tests: el mensaje válido llega a la fachada con `eventId` y `newVersion`; los defectuosos no la llaman ni lanzan;
  el fallo de la fachada se propaga; el topic de la anotación es `${catedra.catalog-topic}`.
