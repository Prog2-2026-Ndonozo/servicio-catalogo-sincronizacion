# ADR-0036: Solo el servicio de turnos consume el catálogo; la interfaz KMP no lo consulta

Fecha: 2026-11-13
Estado: aceptada

## Contexto
El ADR-0035 dejó abierta la ruta de la interfaz KMP hacia la búsqueda: directo a este servicio o a través del
servicio de turnos, porque ningún documento lo define. Se decide ahora.

Lo que dicen los documentos y es compatible con la decisión:
- El diagrama de reserva (I §17) conecta a KMP **solo con turnos**; el catálogo aparece únicamente como interlocutor
  de turnos ("obtener agenda vigente").
- Los filtros "deberán poder utilizarse desde la interfaz KMP" (E §4.1): se cumple si turnos los expone a KMP.
- Los dos servicios se comunican con contratos explícitos protegidos con JWT (E §3), y turnos debe obtener de este
  servicio "la información vigente que necesite" (E §4.2). Turnos no puede duplicar una réplica del catálogo para
  búsquedas (E §3), pero reenviar la consulta no es una réplica.

## Opciones consideradas
1. **KMP llama directo a este servicio y a turnos.**
   + Un salto menos en la búsqueda.
   − Este servicio debe validar tokens de usuario final que emite turnos (ADR-0035, decisión 2) y exponer a la
     interfaz sus endpoints; KMP maneja dos servidores.
2. **KMP solo habla con turnos, y turnos consulta a este servicio.**
   + Este servicio solo atiende a un cliente conocido y valida un único tipo de token (el de servicio).
   + KMP usa un solo servidor; la propiedad por usuario (E §4.2) se aplica en un solo lugar.
   − Turnos expone y mantiene el reenvío de la búsqueda y de las categorías; un salto más; si turnos cae, KMP no
     busca.

## Decisión
Opción 2. **La interfaz KMP no consulta a este servicio.** El único cliente HTTP de este servicio es el servicio de
turnos y reservas, con autenticación entre servicios. Este servicio recibe además notificaciones de la cátedra por
Kafka (no es un cliente HTTP).

Consecuencias directas sobre decisiones anteriores:
- **ADR-0035, decisión 1** ("la búsqueda con filtros la consume la interfaz KMP") queda reemplazada: la consume
  turnos, en nombre de KMP. El pendiente "ruta de KMP hacia la búsqueda" queda resuelto.
- **ADR-0035, decisión 2** (registro y login en turnos) sigue vigente, con una consecuencia nueva: **este servicio
  nunca recibe ni valida tokens de usuario final**. Solo valida el token con que turnos se identifica.
- **Qué consume turnos:** la búsqueda, las categorías, la agenda vigente de un profesional y, opcionalmente, el
  estado de sincronización. Todos son contratos entre servicios y se documentan como tales (E §12).
- **CORS** deja de tener cualquier destinatario: ningún navegador ni app llega a este servicio (refuerza la decisión
  de no implementarlo).

## Pendiente
- **Autenticación entre servicios (#10):** resuelta en el ADR-0037 (JWT técnico emitido por turnos, firmado con RS256).

## Consecuencias
- Turnos debe escribir el reenvío de la búsqueda y de las categorías hacia KMP: trabajo en el otro repositorio.
- Este servicio puede ser estricto: sin token válido de servicio, ninguna ruta de datos responde.
- Las rutas actuales (`/api/professionals/search`, `/api/professional-categories`) se mantienen. Un prefijo
  `/api/internal/...` sería solo cosmético y se evaluará con el contrato de turnos.
- Interpretación a confirmar con la cátedra el 17 de noviembre: que "utilizarse desde la interfaz KMP" (E §4.1) se
  cumple a través de turnos. Lo respalda el diagrama de reserva de I §17.
