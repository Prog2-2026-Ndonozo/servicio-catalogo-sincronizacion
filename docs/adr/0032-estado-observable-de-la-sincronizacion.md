# ADR-0032: Estado observable de la sincronización en PostgreSQL

Fecha: 2026-11-08
Estado: aceptada

## Contexto
El enunciado pide que el servicio pueda "informar el estado y los errores de sincronización" (E §4.1) y que se
detecte y se recupere una discontinuidad (E §6.2). Hoy ese estado solo existe en el log: no hay forma de consultar
si la última sincronización salió bien, si el catálogo se reconstruyó por una discontinuidad o si falló. El backlog
(#7) nombra tres estados: `OK`, `RECONSTRUIDO_POR_DISCONTINUIDAD` y `ERROR`, y deja su exposición por REST para un
issue posterior.

Hay tres decisiones: dónde se guarda el estado, quién lo escribe (en particular `ERROR`, porque el trabajo que falla
revierte su transacción) y cuál es el estado de una base recién creada.

## Opciones consideradas

### Dónde se guarda
1. **Tabla propia de una sola fila en PostgreSQL** (`sync_state`).
   + Persiste entre reinicios y vive junto al catálogo, que es la fuente local de verdad.
   + Mismo patrón que `catalog_version` (fila única, `CHECK (id = 1)`).
   − Una migración y un puerto/adaptador más.
2. **Columnas nuevas en `catalog_version`.**
   + Sin tabla nueva.
   − Mezcla un dato del catálogo (qué versión tengo) con uno operativo (cómo salió la última sincronización), que
     cambian por motivos distintos.
3. **Redis, en el namespace `alumnos:{groupId}:*`** (I §14.6 lo sugiere para diagnósticos, por ejemplo
   `diagnostics:last-sync`).
   + Sin migración; admite TTL.
   − Es el Redis compartido de la cátedra: puede reiniciarse sin aviso y no es una fuente fiable.
   − Exige permiso de escritura y vuelve de escritura un adaptador que hoy es de solo lectura.
4. **Solo log o memoria.**
   − No sobrevive a un reinicio y no se puede exponer de forma confiable.

### Quién escribe el estado
1. **El caso de uso, dentro de su transacción.**
   − Un `ERROR` se revertiría junto con el trabajo que falló y desaparecería.
2. **El caso de uso, con una transacción nueva e independiente.**
   − Cada caso de uso tendría que conocer el estado y, en el snapshot, saber por qué corrió.
3. **La fachada `CatalogSyncService`** (no es transaccional), con una transacción corta propia.
   + Ya tiene los puntos justos: el `try/catch` de `FullSyncRequiredException` y el resultado de cada llamada.
   + Sobrevive a la reversión del trabajo.
   − La escritura queda fuera de la transacción de los datos: si falla justo ahí, el estado queda desactualizado.
4. **El *recoverer* del listener de Kafka** (ADR-0030).
   − Solo se ejecutaría al agotar los reintentos y no cubriría el arranque ni el snapshot.

### Estado de una base recién creada
1. **Cuarto estado `SIN_SINCRONIZAR`, con la fila sembrada.**
   + Dice la verdad; la fila siempre existe y nada devuelve `null` (mismo criterio que `catalog_version`, ADR-0014).
   − Se aparta de los tres estados del backlog.
2. **Sembrar `OK`.**
   − Es falso: nada se sincronizó todavía.
3. **Sin fila hasta la primera sincronización.**
   − Obliga a quien lea el estado a manejar la ausencia.

## Decisión
- Tabla `sync_state` de una sola fila (migración `V3__crear_sync_state.sql`): `id` (`CHECK (id = 1)`), `status`,
  `detail` (texto, admite nulo) y `updated_at` (`DEFAULT now()`). `status` tiene un `CHECK` con los cuatro valores.
  La fila se siembra con `SIN_SINCRONIZAR`.
- Enum de dominio `SyncStatus` y un puerto de salida para registrar el estado y leer el actual. La implementación es
  un adaptador JPA.
- **La fachada escribe el estado**, cada vez en una transacción corta propia y siempre **después** de que termina
  el trabajo. Ante un fallo registra `ERROR` y **vuelve a lanzar la excepción**, para que Spring Kafka siga con sus
  reintentos (ADR-0030).
- **Significado de los estados:**

| Estado | Significa | Cuándo se escribe |
|---|---|---|
| `SIN_SINCRONIZAR` | Nunca terminó una sincronización con éxito | Valor inicial de la fila |
| `OK` | La última sincronización terminó bien | Tras un snapshot de arranque o un evento incremental aplicado |
| `RECONSTRUIDO_POR_DISCONTINUIDAD` | La última sincronización fue un snapshot completo pedido por una discontinuidad (versión local fuera de la ventana o versión intermedia ausente, ADR-0029 y ADR-0031) | Tras terminar ese snapshot de respaldo |
| `ERROR` | Falló el **último intento** de sincronizar | En cada intento fallido, incluido el del arranque |

- `ERROR` describe el último intento y no que se haya perdido información: con los reintentos puede pasar a `ERROR`
  y volver a `OK` en segundos. El siguiente éxito lo reemplaza.
- No se escribe nada cuando un evento se ignora (versión ya aplicada o `eventId` repetido) ni cuando el arranque
  detecta que la base ya estaba sincronizada.
- **`detail`:** texto breve y seguro. En `ERROR`, el nombre de la excepción y su mensaje, recortados; nunca la traza
  ni credenciales (ADR-0020, ADR-0022). En `RECONSTRUIDO_POR_DISCONTINUIDAD`, el motivo de la discontinuidad. En
  `OK` y `SIN_SINCRONIZAR`, nulo.
- **Un fallo al registrar el estado no cambia el resultado de la sincronización:** se registra como `warn` y no
  reemplaza la excepción original ni deshace un trabajo ya confirmado.

## Consecuencias
- Desviación del backlog: aparece el estado `SIN_SINCRONIZAR`, que los tres del backlog no contemplan.
- Hay una ventana mínima en la que el catálogo ya está aplicado pero el estado todavía no se actualizó (o su
  escritura falló). El siguiente evento la corrige. Es el costo de no escribir dentro de la transacción de los datos.
- Cada evento que se aplica suma una escritura corta en `sync_state`.
- Hibernate corre en `ddl-auto: validate`: la entidad debe coincidir con la tabla. `status` se guarda como texto.
- La exposición por REST (backlog #12, E §4.1) leerá este estado a través del puerto; no forma parte de este ADR.
- Tests: la fachada registra `OK` tras un evento aplicado, `RECONSTRUIDO_POR_DISCONTINUIDAD` tras el snapshot de
  respaldo y `ERROR` (relanzando la excepción) ante un fallo; no escribe nada ante un evento ignorado; un fallo del
  registro no oculta el resultado. Un test contra PostgreSQL real comprueba la migración, la fila sembrada y el
  `CHECK` de `status`.
