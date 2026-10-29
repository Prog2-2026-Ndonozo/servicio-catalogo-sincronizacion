# ADR-0024: Sincronización completa inicial al arrancar la aplicación

Fecha: 2026-10-29
Estado: aceptada

## Contexto
El caso de uso `ApplyFullSnapshotUseCase` (ADR-0023) sabe aplicar un snapshot pero no cuándo hacerlo. Hace falta
un disparador. El servicio debe tener el catálogo local antes de resolver búsquedas (E §6.1) y la copia local es
de su exclusiva propiedad. La política de recuperación posterior (discontinuidades, reintentos) es del issue #6.

## Opciones consideradas
1. **Al arrancar, solo si la versión local es 0** (base nunca sincronizada).
   + Sin interfaz expuesta ni seguridad adicional; ocurre una vez; reutiliza la versión 0 como "base vacía"
     (ADR-0014).
   − Si la cátedra no responde en ese momento, el catálogo queda vacío hasta una nueva oportunidad (ADR-0025).
2. **Endpoint administrativo** que dispare la sincronización.
   − Necesita autenticación y autorización, que llegan con el issue #10.
3. **Sincronización periódica** (scheduler).
   − Repite pedidos innecesarios: el camino incremental (Kafka/Redis) mantiene el catálogo al día.
4. **Perezosa**, en la primera búsqueda.
   − Una petición de usuario pagaría la latencia y el riesgo de una llamada de hasta 35 s.

## Decisión
Opción 1, con tres piezas que respetan la arquitectura hexagonal:
- `StartupSyncRunner` (`infrastructure/startup`, `ApplicationRunner`): adaptador primario. Se ejecuta una vez,
  cuando el contexto terminó de levantar, y solo llama a la fachada.
- `CatalogSyncService.synchronizeIfEmpty()` (`application/service`, `@Service`): fachada. Lee la versión local
  y, si es 0, invoca `ApplyFullSnapshotUseCase`; devuelve `true` si sincronizó y `false` si no hizo falta. Los
  drivers entran por la fachada y no por el puerto.
- Propiedad `catedra.sync-on-startup` (por defecto `true`; `matchIfMissing = true`). Con `false` el runner no se
  crea.

## Justificación
La regla "solo si está vacío" es una decisión de orquestación, no de negocio: vive en la fachada y así cualquier
otro disparador futuro (endpoint, reintento) la reutiliza sin copiarla. La propiedad evita llamadas a la cátedra
en tests, en desarrollo sin red o con la cátedra en mantenimiento.

## Consecuencias
- Si la versión local es mayor a 0 el arranque no pide nada; ponerse al día es trabajo del camino incremental y
  de la recuperación del issue #6.
- Un endpoint de disparo manual queda diferido hasta tener seguridad (issue #10).
- Tests: la fachada y el runner con Mockito, y el interruptor de la propiedad con `ApplicationContextRunner`
  (existe / no existe el bean).
- README documenta la propiedad en "API de la cátedra".
