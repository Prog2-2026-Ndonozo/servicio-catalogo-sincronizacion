# ADR-0031: Versión intermedia ausente como discontinuidad (`Optional` en el puerto de lectura)

Fecha: 2026-11-08
Estado: aceptada

## Contexto
El caso de uso incremental recorre `changes:{v}` desde la versión local + 1 hasta la del evento (ADR-0028). Si una de
esas claves no existe, falta la versión que conecta el estado local con el nuevo: es una **discontinuidad** y el
enunciado exige sincronización completa (E §6.2: "una discontinuidad, una versión fuera de la ventana disponible o
un estado local que no puede verificarse"; I §18.2).

El ADR-0029 resolvió el caso en que la versión local queda por debajo de `oldestAvailableVersion`, que se detecta
antes de leer nada. Queda el caso en que, ya dentro de la comprobación, falta una versión a mitad del recorrido: la
ventana avanzó mientras se leía, o la clave se perdió en Redis.

Hoy `CatedraRedisAdapter.fetchChanges` trata igual una clave inexistente y un JSON ilegible: ambas lanzan
`InvalidCatalogDataException` (ADR-0027), el listener reintenta y finalmente descarta el evento. Solo la ausencia es
una discontinuidad; un JSON ilegible es otro problema.

## Opciones consideradas
1. **Excepción propia para la ausencia** (`ChangesNotAvailableException`), lanzada por el adaptador y traducida a
   `FullSyncRequiredException` por el caso de uso.
   + No cambia la firma del puerto.
   + Separa la ausencia del dato ilegible.
   − Usa una excepción para un resultado normal del protocolo (la ventana avanza), no para un error.
   − El contrato del puerto queda solo en un comentario; el compilador no obliga a tratar el caso.
2. **El puerto devuelve `Optional<CatalogChanges>`**: vacío significa clave ausente.
   + La ausencia queda expresada en el tipo: quien llama está obligado a decidir qué hacer.
   + Las excepciones se reservan para fallos reales (Redis caído, JSON ilegible).
   − Cambia la firma del puerto: hay que ajustar el adaptador, el caso de uso y sus tests.
3. **Releer la ventana ante cualquier `InvalidCatalogDataException`** y pedir snapshot si la versión local quedó por
   debajo de `oldestAvailableVersion`.
   + No toca el adaptador.
   − Solo detecta que la ventana avanzó: si una clave falta **dentro** de la ventana (datos perdidos en Redis) no
     se detecta.
   − Atrapa una excepción demasiado amplia y puede ocultar datos realmente incoherentes.
   − Depende de una segunda lectura que puede cambiar entre un intento y otro.

## Decisión
Opción 2.

- `CatedraCatalogReadPort.fetchChanges(long version)` devuelve `Optional<CatalogChanges>`. `CatedraRedisAdapter`
  devuelve `Optional.empty()` cuando `GET catedra:sync:changes:{v}` no trae valor.
- `ApplyIncrementalChangeUseCaseImpl` convierte el vacío en `FullSyncRequiredException` con un mensaje que indica la
  versión que falta (sin datos sensibles). La fachada ya captura esa excepción y pide el snapshot (ADR-0029). El
  caso de uso no había guardado nada todavía cuando la lanza, así que la transacción se revierte sin efectos.
- **Sigue siendo `InvalidCatalogDataException`** (reintento y descarte, ADR-0026 y ADR-0030):
  - un `changes:{v}` presente pero ilegible o `null` como JSON, o que declare una versión distinta de la pedida;
  - un ID de `changes` que no está en el Hash de entidades: el contrato dice que las bajas son lógicas y la
    entidad permanece (I §14.4), así que su ausencia es una incoherencia y no una discontinuidad de versiones.

## Consecuencias
- Actualiza lo dicho en ADR-0027: "clave `changes:{v}` ausente" deja de ser un dato incoherente.
- Cambian los tests: el adaptador devuelve vacío ante clave ausente (antes lanzaba); los stubs de `fetchChanges` en
  los tests del caso de uso devuelven `Optional`; el caso "falta una versión intermedia" ahora espera
  `FullSyncRequiredException` y no `InvalidCatalogDataException`.
- Si más adelante se decide que un `changes:{v}` ilegible también debe reconstruir (por "estado que no puede
  verificarse", E §6.2), se agrega sin cambiar la firma: el caso de uso o el adaptador lo tratarían como ausencia.
- El snapshot se pide aunque la ausencia sea transitoria (por ejemplo, una clave aún no publicada). Es seguro, solo
  más costoso que reintentar; el contrato indica que la versión está publicada antes de avisar por Kafka.
- Tests: ausente a mitad del recorrido pide snapshot sin guardar versión ni `eventId`; presente pero ilegible sigue
  siendo `InvalidCatalogDataException`.
