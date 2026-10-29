# ADR-0025: El servicio sigue activo si la cátedra no responde al arrancar

Fecha: 2026-10-29
Estado: aceptada

## Contexto
La sincronización inicial (ADR-0024) depende de un servicio externo. La cátedra puede estar caída, responder con
error o tardar más que el tiempo de espera (ADR-0019, ADR-0022). El catálogo y las búsquedas son del servicio, y
un servicio de la cátedra no debería decidir si el nuestro está en pie o no. Además el contrato exige no dejar
estado local incoherente (E §5.15).

## Opciones consideradas
1. **Capturar `CatedraUnavailableException` en el arranque, registrar una advertencia y seguir.**
   + Independencia entre servicios; el servicio queda disponible para reintentar la sincronización más tarde.
   − Arranca "sano" pero con catálogo vacío; hay que dejarlo visible.
2. **Dejar que el arranque falle** si la cátedra no responde.
   + Imposible no notar el problema.
   − Una dependencia externa tumba el servicio; en un entorno con reinicios automáticos entraría en bucle.
3. **Reintentar en el arranque hasta lograrlo.**
   − Bloquea el arranque; la política de reintentos es del issue #6.

## Decisión
Opción 1. `StartupSyncRunner` captura **solo** `CatedraUnavailableException` y escribe un `log.warn` con el
status HTTP (si lo hubo), sin la excepción completa ni el JWT (ADR-0020). Cualquier otra excepción se propaga:
en particular `InvalidCatalogDataException`, porque un snapshot con datos inválidos es un problema de datos y no
de disponibilidad (ADR-0014 supone que llegan válidos), y debe ser ruidoso.

La propiedad `catedra.sync-on-startup` (ADR-0024) es un interruptor manual distinto: apaga el intento, mientras
que esta decisión define qué pasa cuando el intento falla.

## Consecuencias
- Con la cátedra caída el servicio arranca con versión 0 y sin datos; las búsquedas devuelven vacío hasta que
  una sincronización posterior tenga éxito.
- Hasta que el issue #6 implemente reintentos, la recuperación es reiniciar el servicio cuando la cátedra esté
  disponible. La transacción del ADR-0023 garantiza que un intento fallido no deja datos a medias.
- Más adelante, el estado de sincronización (issue #7) debería exponer que el catálogo nunca se sincronizó.
- Tests: el runner no propaga `CatedraUnavailableException` (con y sin status) y sí propaga
  `InvalidCatalogDataException`.
