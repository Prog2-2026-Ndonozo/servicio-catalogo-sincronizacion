# ADR-0027: Lectura de Redis de la cátedra

Fecha: 2026-11-05
Estado: aceptada

## Contexto
El evento `CatalogUpdated` solo notifica una versión nueva (I §15.3); el detalle está en Redis (I §14). Para
aplicar un cambio incremental hace falta leer la ventana de versiones (`catedra:sync:metadata`), los IDs afectados
(`catedra:sync:changes:{v}`) y el estado vigente de cada ID en los Hashes de categorías, profesionales y horarios.
Las entidades deshabilitadas permanecen en los Hashes y representan bajas lógicas (I §14.4). El acceso es solo
lectura sobre `catedra:sync:*` (I §14.1).

## Opciones consideradas
1. **DTOs propios en `catedraredis`** para los JSON de Redis.
   + Los adaptadores no dependen entre sí; si un contrato cambia, se toca un solo paquete.
   − Tres records casi iguales a los de `catedrarest`.
2. **Reutilizar los DTOs de `catedrarest`.**
   + Menos código.
   − Acopla dos adaptadores que evolucionan por separado.
3. **Leer cada entidad con un comando por ID.**
   − Un viaje a Redis por ID; con muchos cambios la latencia crece sin necesidad.

## Decisión
- **Puerto `CatedraCatalogReadPort`** (dominio, salida) con cinco lecturas: ventana de versiones, cambios de una
  versión y, por tipo de entidad, el estado vigente de una colección de IDs. El dominio incorpora `VersionWindow`
  (versión vigente y más antigua disponible; no negativas y `oldest <= current`) y `CatalogChanges` (versión
  positiva e IDs por tipo, validados y copiados de forma inmutable).
- **Adaptador `CatedraRedisAdapter`** sobre `StringRedisTemplate`: la ventana sale de un único `HGETALL` de
  `catedra:sync:metadata` (las dos versiones quedan consistentes entre sí); las entidades se piden con un
  `HMGET` por tipo. Los IDs repetidos se piden una vez y el orden de salida es el de entrada.
- **DTOs propios** (opción 1) y un `CatedraRedisMapper` que traduce DTO a dominio, con el mismo criterio que
  `SnapshotMapper` (descripción nula de una categoría = texto vacío).
- **Errores:** un fallo de Redis (`DataAccessException`) se traduce a `CatedraUnavailableException` con la causa
  adjunta y un mensaje fijo, sin detalles de la conexión ni credenciales (ADR-0022). Un dato incoherente lanza
  `InvalidCatalogDataException`: clave `changes:{v}` ausente, un ID pedido que no está en el Hash (se informan
  todos los faltantes juntos), JSON ilegible o `null`, campos que el dominio rechaza, o unos cambios que declaran
  una versión distinta de la pedida. Solo la llamada a Redis se envuelve como indisponibilidad: un dato mal
  formado no se confunde con un fallo transitorio.
- **Sin reintentos en el adaptador.** Reintentar es decisión del caso de uso o del listener.
- **Conexión por `host` y `port`:** `CATEDRA_REDIS_HOST` y `CATEDRA_REDIS_PORT`, más `username` y `password`. No se
  usa `spring.data.redis.url`: con `url` presente, Spring Boot ignora `username` y `password` ("Overrides host, port,
  username, password, and database") y la conexión se abre sin autenticar. Poner las credenciales dentro de la URL
  las expondría en cualquier registro de configuración y exigiría codificar los caracteres especiales.
- **Timeout de comando:** `spring.data.redis.timeout: 5s` (el valor por defecto de Lettuce es de 60 s), con el
  mismo criterio de ADR-0019 para el cliente REST.

## Consecuencias
- **Conexión perezosa:** Lettuce no abre la conexión al arrancar, sino con el primer comando. La aplicación
  arranca aunque Redis esté caído (coherente con ADR-0025), y una configuración errónea (por ejemplo una
  contraseña incorrecta) recién se detecta en la primera lectura, cuando llega un evento.
- **Antecedente (2026-11-06):** con `url` más `username` y `password`, la primera prueba con eventos reales falló con
  `NOAUTH HELLO must be called with the client already authenticated`: los tests unitarios no podían detectarlo
  y la conexión perezosa lo postergó hasta la primera lectura. Cada evento agotó sus reintentos y se descartó.
- **Actualización (ADR-0031):** una clave `changes:{v}` ausente ya no es un dato incoherente: el puerto devuelve
  `Optional` y el caso de uso la trata como discontinuidad (snapshot completo).
- Que falte un ID es una incoherencia con el contrato, no un dato opcional: el caso de uso no debe avanzar la
  versión local si la lectura no está completa (I §14.5).
- Las listas devueltas no garantizan un orden respecto de los IDs pedidos más allá del de entrada; el caso de uso
  guarda por ID y no debe depender de él.
- Tests unitarios con Redis simulado cubren la lógica y los errores, no los comandos reales. Esos se verifican a
  mano contra el Redis de la cátedra (solo lectura); un test con Redis real (Testcontainers) queda pendiente.
