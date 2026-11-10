# ADR-0034: Significado de "disponibilidad" en el filtro de búsqueda

Fecha: 2026-11-09
Estado: aceptada (supuesto a confirmar con la cátedra)

## Contexto
La búsqueda debe ofrecer como mínimo filtros por categoría, nombre, estado habilitado y disponibilidad, resueltos
con la copia local y utilizables desde la interfaz KMP (E §4.1). El enunciado no define qué significa
"disponibilidad" para este filtro.

El contrato acota lo posible. Quien "construye la disponibilidad a partir de horarios y ocupaciones" es el
servicio de turnos (E §4.2, E §7): pide la agenda vigente a este servicio, consulta las ocupaciones a la API de la
cátedra (`GET /api/appointment-occupancies`) y devuelve los slots libres (I, diagrama de la reserva). Este servicio
solo tiene las reglas semanales (`WeeklySchedule`: día, hora de inicio y fin, duración del turno y `enabled`) y no
tiene ocupaciones.

## Opciones consideradas
1. **Tiene algún horario habilitado** (booleano, sin fecha).
   + Trivial.
   − Casi todo profesional lo cumple: no sirve para elegir.
2. **Atiende un día de la semana** (`dayOfWeek`).
   + Simple y preciso.
   − La interfaz trabaja con fechas, no con días de la semana.
3. **Atiende en una fecha** (`availableOn`): tiene un horario habilitado para el día de la semana de esa fecha.
   + Natural para un selector de fecha; el cálculo es directo con datos locales.
   − Hay que dejar claro que no garantiza turnos libres.
4. **Atiende en una fecha y franja horaria.**
   + Más preciso.
   − Parece prometer capacidad que no se puede conocer sin ocupaciones; más casos borde.
5. **Disponibilidad real, descontando ocupaciones.**
   + Es lo que el usuario final espera ver.
   − Es responsabilidad del servicio de turnos (E §4.2, E §7); implicaría consultar a la cátedra y duplicar su
     lógica en este servicio.

## Decisión
Opción 3. Un único parámetro opcional `availableOn` (fecha ISO `yyyy-MM-dd`). Si viene, la búsqueda devuelve los
profesionales que tienen al menos un horario semanal habilitado cuyo día de la semana coincide con el de esa fecha.
Si no viene, no se filtra por disponibilidad.

Reglas:
1. **Solo cuentan los horarios con `enabled = true`.** Uno deshabilitado es una baja lógica (I §14.4).
2. **El filtro es independiente de los demás.** No implica que el profesional ni su categoría estén habilitados:
   eso lo decide el filtro de estado habilitado, que el cliente combina como necesite (KMP envía `enabled=true`).
3. **La fecha no lleva zona horaria:** es un `LocalDate` y el día de la semana se toma de la fecha tal cual. Los
   horarios semanales no tienen vigencia por fechas, así que cualquier fecha se corresponde con su día.
4. **Una fecha pasada no se rechaza.** Este servicio no tiene una regla para eso; validar que se pueda reservar es
   responsabilidad de quien arma la reserva.

**Redacción del contrato:** el filtro significa "atiende ese día", no "tiene turnos libres". La documentación del
endpoint y la interfaz no deben presentarlo como disponibilidad de turnos.

## Consecuencias
- Es una **interpretación propia** de un punto que E §4.1 no define. Se consulta con la cátedra en la reunión del 17
  de noviembre. Si la respuesta es otra, cambia la consulta que implementa el filtro y no el resto del endpoint.
- El resultado es un pre-filtro: el servicio de turnos sigue construyendo los slots con las ocupaciones.
- Resuelve parte del pendiente "efecto de una categoría deshabilitada sobre sus profesionales" para este filtro: no
  influye. Su efecto sobre el resto de la búsqueda sigue pendiente.
- Una búsqueda con `availableOn` necesita la relación entre profesionales y horarios (`professional_id`): la
  consulta debe resolverse en la base, sin cargar todos los profesionales en memoria.
- Tests: un profesional con horario habilitado ese día aparece; uno con solo horarios deshabilitados o de otros días
  no; sin `availableOn` no se filtra; combinado con los demás filtros se aplican todos.
