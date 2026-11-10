# ADR-0035: Consumidores de la búsqueda, ubicación del login y filtro por nombre

Fecha: 2026-11-10
Estado: aceptada (con dos puntos abiertos, ver "Pendiente")

## Contexto
Antes de implementar la búsqueda (#9) hacía falta fijar quién la consume, dónde viven el registro y el login del
usuario final y qué significa el filtro por nombre. Se revisaron completos E (PROJECT_STATEMENT) e I
(INTEGRATION_REFERENCE).

**Quién consume qué, según los documentos:**
- La búsqueda con filtros es para la interfaz KMP: los filtros "deberán poder utilizarse desde la interfaz KMP"
  (E §4.1) y el usuario final "busca profesionales" (I §2). La búsqueda debe resolverse sobre la copia local, nunca
  consultando a la cátedra (E §6.2), y el servicio de turnos no puede duplicar una réplica del catálogo "para
  búsquedas o nuevas reservas" (E §3): la búsqueda vive en este servicio.
- El servicio de turnos consume el catálogo para otra cosa: "obtener la información vigente que necesite" (E §4.2) y
  las "reglas semanales vigentes" (E §7). El diagrama de reserva (I §17) muestra a turnos pidiendo la **agenda
  vigente** y recibiendo "profesional y horarios locales"; ese diagrama no tiene ningún paso de búsqueda. Este
  servicio es "la única fuente local vigente para consultar profesionales y sus reglas de agenda" (E §4.1).

**Registro y login del usuario final:** E §3.2 los asigna al "backend desarrollado por el alumno" sin indicar el
servicio.

## Opciones consideradas
Para el filtro por nombre:
1. **Igualdad exacta.** − Un usuario que escribe "ana" no encuentra a "Ana Torres" ni a "Mariana".
2. **Prefijo.** − No encuentra por apellido ni por una parte intermedia del nombre.
3. **Contiene el texto, sin distinguir mayúsculas, sobre nombre y apellido.** + Cubre la forma en que se busca una
   persona; − una consulta `LIKE '%texto%'` no puede usar un índice común (aceptable con el volumen de un catálogo
   de profesionales).
4. **Contiene el texto, sin distinguir mayúsculas ni tildes.** + Mejor experiencia con nombres en español; − exige la
   extensión `unaccent` de PostgreSQL o una columna normalizada. Se descarta por ahora: los datos actuales no
   llevan tildes y sería ingeniería prematura.

Para el login: **en este servicio** o **en el servicio de turnos**. El enunciado no los distingue; ver Decisión.

## Decisión
1. **La búsqueda con filtros la consume la interfaz KMP.** El servicio de turnos **no** consume la búsqueda: consume
   la **agenda vigente de un profesional**, una consulta por ID aparte (profesional, su estado y sus horarios
   semanales, con los indicadores `enabled`), que no figuraba en el backlog y se agrega. La tabla de puertos de
   entrada de `arquitecturaHexagonalServicioCatalogo.md` se corrige en ese sentido: se quita la suposición de que
   turnos llama a la búsqueda.
2. **El registro y el login del usuario final viven en el servicio de turnos y reservas.** Consecuencia: el JWT del
   usuario final lo emite ese servicio; este servicio debe poder **validar su firma, vigencia y permisos** (E §9)
   para proteger sus endpoints.
3. **Filtro por nombre (opción 3):** un único texto que se busca dentro del nombre y del apellido del profesional,
   ignorando mayúsculas y minúsculas. Las tildes **no** se ignoran por ahora (`jose` no encuentra a "José"): los
   datos actuales no las tienen, y se agrega si hace falta (opción 4). La coincidencia es por contenido, no por
   igualdad ni prefijo, y se aplica sobre el nombre y el apellido juntos, de modo que `ana torres` y `torres`
   también encuentran a "Ana Torres". El texto se recorta de espacios antes de buscar.
4. **Sin paginación por ahora:** la búsqueda devuelve todos los resultados, ordenados por apellido, nombre e id. El
   contrato no la pide y el catálogo es chico.
5. **La interfaz también necesita listar las categorías** para ofrecer el filtro por categoría: se agrega un
   endpoint de lectura de categorías al #9.

## Pendiente (se decide aparte, con más análisis)
- **Ruta de KMP hacia la búsqueda:** directo a este servicio o a través del servicio de turnos. Ningún documento lo
  define.
- **Autenticación entre servicios:** propagar el JWT del usuario o usar un JWT técnico (E §9 lo deja a cargo del
  alumno y exige justificarlo).

## Consecuencias
- Este servicio queda con dos tipos de consumidor y, por lo menos, dos endpoints: la búsqueda (KMP) y la agenda por
  profesional (turnos). El segundo se suma al backlog.
- Como el login está en turnos, la protección de la búsqueda depende de cómo se validen allí los tokens (clave
  compartida o par de claves): se resuelve junto con los dos puntos pendientes.
- Si más adelante se necesita ignorar tildes, se resuelve en la base: la extensión `unaccent` de PostgreSQL o una
  columna con el nombre normalizado. No se verificó que `unaccent` esté disponible en la imagen de PostgreSQL usada.
- Al escapar el texto del usuario en la consulta (`%` y `_` son comodines de `LIKE`), una búsqueda por `%` no debe
  devolver todo el catálogo.
- Turnos no puede conservar la agenda como catálogo vigente (E §3, E §4.2): cada operación consulta a este servicio.
- Tests: coincidencias por nombre, por apellido, por ambos, sin distinguir mayúsculas, con espacios sobrantes, con
  comodines en el texto y sin resultados.
