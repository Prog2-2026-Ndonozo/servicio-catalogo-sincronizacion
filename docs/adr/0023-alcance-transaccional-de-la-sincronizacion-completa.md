# ADR-0023: Alcance transaccional de la sincronización completa

Fecha: 2026-10-29
Estado: aceptada

## Contexto
E §6.1 exige aplicar el snapshot "de forma consistente" y registrar como versión local exactamente la que
corresponde a los datos almacenados. ADR-0018 fijó el principio: la unidad de trabajo es una versión completa,
dentro de una transacción, con claves foráneas diferidas. Faltaba decidir dónde empieza y termina esa
transacción y en qué orden se escribe. El caso de uso `ApplyFullSnapshotUseCaseImpl` hace tres cosas en
secuencia: pide el snapshot por `CatedraSnapshotPort`, guarda categorías, profesionales y horarios por
`CatalogRepositoryPort`, y guarda la versión.

## Opciones consideradas
1. **Todo el caso de uso en una transacción** (`@Transactional` en `applyFullSnapshot()`), incluida la llamada
   HTTP a la cátedra.
   + Una sola anotación; cualquier fallo, incluso del último paso, deshace todo.
   − La conexión a la base queda tomada mientras dura la llamada HTTP (hasta 5 s de conexión + 30 s de lectura,
     ADR-0019).
2. **Pedir el snapshot fuera de la transacción** y abrir la transacción solo para escribir.
   + No retiene una conexión durante la espera de red.
   − El caso de uso recibiría el snapshot como parámetro, o habría que dividirlo en dos; el puerto de entrada y
     la fachada se complican sin una necesidad medida.
3. **Una transacción por colección.**
   − Un fallo a mitad de camino dejaría datos nuevos con la versión vieja: es justo lo que E §6.1 prohíbe.

## Decisión
Opción 1. `applyFullSnapshot()` lleva `@Transactional` de Spring (`org.springframework.transaction.annotation`,
no la de Jakarta) y cualquier `RuntimeException` deshace la transacción.

Orden de escritura dentro de la transacción:
1. categorías, 2. profesionales, 3. horarios semanales, 4. **versión local, siempre al final**.

Las escrituras son upserts por ID (ADR-0018), así que reaplicar el mismo snapshot no cambia el resultado.

## Justificación
- El orden de las tres colecciones no se debe a las claves foráneas, que son diferidas y se verifican al
  confirmar. Se mantiene por claridad y porque la versión se guarda al final como **red de seguridad**: aunque
  alguien rompiera la transacción por error, la versión nunca avanzaría antes que los datos.
- Esta sincronización corre al arrancar y una vez (ADR-0024), no por pedido de usuarios; retener una conexión
  durante 35 s como máximo es un costo aceptable. Medir antes de optimizar: si el issue #6 introduce
  reintentos o sincronizaciones concurrentes, se reevalúa la opción 2.
- Un `CatedraUnavailableException` o `InvalidCatalogDataException` ocurre antes de cualquier escritura (el
  snapshot se pide y se traduce primero), por lo que no se guarda nada.

## Consecuencias
- La versión local avanza si y solo si categorías, profesionales y horarios se guardaron.
- Los tests del caso de uso verifican el orden y que la versión no se guarda si un paso falla (con mocks) y que
  el método tiene `@Transactional` (por reflexión). **No prueban el deshacer real de la base**: eso requiere un
  PostgreSQL de verdad (test de integración con Testcontainers), pendiente.
- El caso de uso **no elimina** filas locales ausentes del snapshot. Sobre una base vacía no importa; la
  reconstrucción tras una discontinuidad (issue #6, I §18) tendrá que decidir si limpia antes de aplicar.
- La fachada que decide cuándo sincronizar no abre transacciones: esa responsabilidad es solo del caso de uso.
