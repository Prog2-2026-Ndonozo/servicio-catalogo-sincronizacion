# ADR-0033: Se mantiene un solo módulo `catalog` hasta la entrega; desvíos conocidos respecto de la skill

Fecha: 2026-11-09
Estado: aceptada

## Contexto
Al implementar la sincronización incremental (#12 a #16) y el estado observable (ADR-0032) quedó la duda de si la
sincronización debería ser un módulo (*slice*) separado de `catalog`. El módulo `catalog` hoy contiene sobre todo
sincronización: listener de Kafka, adaptadores de Redis y REST, deduplicación, estado, versión, fachada y runner de
arranque. La búsqueda (Milestone 4) todavía no existe. La decisión de un solo módulo viene de
`docs/arquitecturaHexagonalServicioCatalogo.md`.

Se contrastó el código con la skill `hexagonal-catalogo` (`../../hexagonal/.agents/skills/`), que AGENTS.md indica
como autoridad ante dudas de diseño hexagonal. La skill no obliga a un solo módulo ni lo prohíbe: define el *slice*
como un agregado, pide que entre slices se dependa del puerto de entrada o del modelo, y recomienda duplicar antes
de extraer.

Fechas: este servicio debe quedar funcional el 17 de noviembre, con avances del servicio de turnos y reservas; la
entrega de ambos servicios es el 29 de noviembre.

## Opciones consideradas
1. **Partir ya en `catalog` y `sync`.**
   + Separa responsabilidades antes de que crezca la búsqueda.
   − Estimado: ~90 archivos tocados (todos cambian de paquete o de imports), un puerto de entrada nuevo en
     `catalog`, reglas de ArchUnit y fixtures a rehacer, y la transacción de ADR-0028 cruzando la frontera.
   − Compite con el plazo del 17 y con el avance del otro servicio.
2. **Mantener un solo módulo, terminar los milestones y evaluar la migración sobre el servicio ya funcionando.**
   + El plazo se dedica a funcionalidad y a las evidencias mínimas (E §11).
   + Una migración posterior se hace con 328 tests como red de seguridad y sin urgencia de entrega.
   − La deuda de diseño sigue visible hasta entonces.

## Decisión
Opción 2. Se sigue en el módulo `catalog`, se completan los milestones restantes y se deja el servicio funcional
para el 17 de noviembre. Si se decide migrar a módulos separados, se hace después, sobre el servicio ya funcionando
y de forma mecánica. La evaluación se retoma después de la reunión con la cátedra del 17 de noviembre.

Se hizo antes un cambio barato que reduce el costo de una futura separación: la versión aplicada salió de
`CatalogRepositoryPort` hacia su propio puerto, `CatalogVersionPort`.

## Desvíos conocidos respecto de la skill
Se registran sin decidir que sean correctos; se revisan junto con la migración.

1. **La fachada `CatalogSyncService` decide, no solo delega:** `synchronizeIfEmpty` compara versiones con un puerto
   de salida, y `applyCatalogUpdate` elige pedir el snapshot al capturar `FullSyncRequiredException` (ADR-0029).
   ADR-0032 suma ahí el registro del estado. La skill indica que esa lógica baja a un caso de uso.
2. **El caso de uso incremental lanza `FullSyncRequiredException`** para avisar que hace falta un snapshot; la skill
   prefiere que el caso de uso informe un resultado y que quien lo llama decida.
3. **Ubicación de `FullSyncRequiredException`:** está en `domain/exception`; por la skill correspondería a
   `application/exception`. Las otras dos excepciones tienen respaldo en ADR-0016 y ADR-0022.
4. **Tests con PostgreSQL real (Testcontainers) en lugar de H2:** la skill propone H2; el enunciado lo prohíbe
   como persistencia principal (E §3) y la propia skill admite Testcontainers para dependencias de dialecto.

## Consecuencias
- Hasta el 17, el desarrollo sigue con la estructura actual y las reglas de ArchUnit vigentes.
- Los desvíos 1 a 3 quedan como candidatos a corregir durante la migración o antes, si hay tiempo.
- Si el equipo docente pide separar módulos fuera del plazo de entrega, el trabajo ya no compite con el servicio de
  turnos y reservas.
