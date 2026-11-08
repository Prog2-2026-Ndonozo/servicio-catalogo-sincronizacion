# ADR-0029: Snapshot completo de respaldo cuando la versión local queda fuera de la ventana

Fecha: 2026-11-06
Estado: aceptada

## Contexto
La sincronización incremental lee `catedra:sync:changes:{v}` desde la versión siguiente a la local hasta la del
evento (ADR-0027, ADR-0028). Esas claves solo existen dentro de la ventana que publica la cátedra
(`oldestAvailableVersion` .. `currentVersion`, I §14.5).

El contrato fija la regla: si la versión local es menor que `oldestAvailableVersion`, faltan incrementos y se debe
obtener un snapshot REST (I §14.2); en el ejemplo, local 3 con `oldestAvailableVersion` 4 **no** se intenta empezar
en `changes:4` (I §18.2). El enunciado lo exige de forma general: ante una discontinuidad, una versión fuera de la
ventana o un estado local que no puede verificarse, hay sincronización completa (E §6.2); y una base local vacía
también pide snapshot (E §6.1).

Hoy `oldestAvailableVersion` es 1, así que el caso no ocurre, pero la ventana avanza con el tiempo y un servicio
apagado el tiempo suficiente quedaría fuera de ella. Sin esta regla, `fetchChanges` fallaría para siempre con una
clave ausente y el catálogo local quedaría desactualizado.

## Opciones consideradas
1. **El caso de uso incremental avisa con una excepción y la fachada pide el snapshot.**
   + El caso de uso conserva una sola responsabilidad (aplicar cambios incrementales).
   + La fachada, que es quien orquesta casos de uso, toma la decisión de política.
   + Cada caso de uso mantiene su propia transacción.
   − Una excepción de dominio más.
2. **El caso de uso incremental llama al caso de uso de snapshot.**
   + No hace falta una excepción nueva.
   − Un caso de uso depende de otro, y el snapshot correría dentro de la transacción del incremental.
3. **No hacer nada: dejar que `fetchChanges` falle y Kafka reintente.**
   − Incumple el contrato (I §14.2, E §6.2). El evento se descarta tras los reintentos y el catálogo no se repara.

## Decisión
Opción 1.

- Se agrega `FullSyncRequiredException` (dominio, `RuntimeException`, mensaje sin datos sensibles).
- `ApplyIncrementalChangeUseCaseImpl`, después de las barreras de versión y de `eventId` (ADR-0028) y antes de leer
  cambios, consulta `fetchVersionWindow()` y lanza `FullSyncRequiredException` si:
  - la versión local es 0 (la base no está sincronizada), o
  - la versión local es **menor** que `oldestAvailableVersion` (estrictamente: local igual a `oldestAvailableVersion`
    sigue siendo incremental, porque `changes:{local+1}` está dentro de la ventana).
- `CatalogSyncService.applyCatalogUpdate` captura **solo** esa excepción, registra un `warn` con el motivo y llama a
  `applyFullSnapshot()`, devolviendo la versión que ese caso de uso aplicó.
- La fachada **no** es `@Transactional`: la transacción del incremental se revierte al salir con la excepción (no
  había escrito nada) y el snapshot corre en su propia transacción (ADR-0023).
- Cualquier otra excepción (Redis o cátedra no disponibles, dato incoherente) se propaga y rige el manejo de
  reintentos de Spring Kafka (ADR-0026).

## Consecuencias
- No se marca el `eventId` como procesado tras el snapshot: si el evento llega de nuevo, la barrera de versión lo
  ignora porque el snapshot ya subió la versión local.
- Cada evento que supera las barreras suma una lectura de `catedra:sync:metadata`.
- Si la ventana avanza mientras se leen los cambios, `changes:{v}` puede desaparecer a mitad del bucle: se lanza
  `InvalidCatalogDataException`, se revierte todo y el reintento de Kafka detecta la brecha en la comprobación
  previa.
- **No resuelto:** si el snapshot trae una versión menor que la del evento (`snapshotVersion < newVersion`), el
  contrato no lo especifica. Redis y el snapshot salen de la misma fuente, por lo que no se espera; el siguiente
  evento lo corregiría. Se revisa si se observa en la práctica.
- Tests: local por debajo de `oldestAvailableVersion` y local 0 lanzan la excepción sin leer cambios ni guardar;
  local igual a `oldestAvailableVersion` sigue por el camino incremental; la fachada pide el snapshot ante la
  excepción y no captura otras.
