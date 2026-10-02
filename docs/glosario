# Glosario del proyecto — turnos distribuidos

> Subí este archivo al Proyecto de claude.ai (o dejalo en `docs/` de cada repo) para no tener que volver a preguntar "¿qué es esto?" cada vez.

## Arquitectura general

**KMP (Kotlin Multiplatform):** un framework que permite compartir código Kotlin entre plataformas (Android, iOS, etc.), aunque en este proyecto solo entregás la app Android. No es "un framework de UI" en sí — la UI la resolvés con lo que uses arriba (Compose, típicamente). Para vos, en la práctica: es una app Android normal escrita en Kotlin, con la etiqueta KMP porque la cátedra deja abierta la puerta a otras plataformas.

**Microservicio / límite de servicio:** cada uno de tus dos backends (catálogo y turnos) es dueño exclusivo de sus datos. Nunca uno lee la base del otro directamente — se hablan por HTTP (contrato REST propio, que vos diseñás) o eventos. Esto es lo que el enunciado llama "no acoplar por código interno".

**Spring Boot / JHipster:** Spring Boot es el framework Java para construir los servicios (controllers, seguridad, persistencia). JHipster es un generador que te arma un Spring Boot + Angular/React ya configurado con usuarios, JWT, etc. — es opcional, pero si lo usás, el modelo de usuario que genera es compatible con lo que pide la cátedra.

## Autenticación y JWT

**JWT (JSON Web Token):** un token firmado que probás en cada request para decir "soy este usuario, con estos permisos", sin que el servidor tenga que guardar sesión. En este proyecto hay **dos JWT completamente distintos y que nunca se mezclan**:
- El JWT **técnico** de tu cuenta de integración ante la cátedra (lo usan tus dos backends para hablar con el servicio central).
- El JWT del **usuario final** (lo emite tu propio backend cuando alguien se registra/loguea en la app KMP).

**`groupId`:** el identificador único de tu proyecto ante la cátedra (no es un grupo de compañeros, es individual). Sirve para aislar tus datos en Redis, Kafka y REST del resto de los alumnos.

## Sincronización de catálogo

**Snapshot:** una foto completa del catálogo (categorías, profesionales, horarios) en un momento dado. Lo pedís cuando arrancás de cero o cuando perdiste el hilo de los cambios incrementales.

**Sincronización incremental:** en vez de traer todo de nuevo, aplicás solo los *cambios* desde tu última versión conocida. Es más eficiente pero exige que los apliques en orden y sin saltarte ninguno.

**Discontinuidad de versión:** si tu copia local está en la versión 3 y el servidor ya no tiene guardado el cambio de la versión 4 (se "cayó" del historial retenido), no podés seguir incrementalmente: tenés que pedir un snapshot nuevo.

## Kafka

**Topic:** un canal con nombre por donde viajan eventos de un mismo tipo (ej: `catedra.catalog.{groupId}`).

**Consumer group:** un identificador que agrupa a tus consumidores para que Kafka sepa desde dónde siguen leyendo. Vos tenés uno asignado (`kafkaConsumerGroupId`).

**Entrega "at-least-once" (al menos una vez):** Kafka puede entregarte el mismo evento más de una vez. Tu código tiene que ser capaz de procesarlo dos veces sin romper nada (ver "idempotencia" abajo).

**`eventId`:** el identificador único de cada evento, pensado justamente para que vos detectes "ya procesé este" y lo ignores la segunda vez.

**Message key:** el campo que Kafka usa para decidir en qué partición va cada mensaje. Mensajes con la misma key mantienen orden entre sí (por eso los eventos de un mismo proceso de reserva comparten `reservationProcessId` como key).

## Redis

**Hash:** un tipo de dato de Redis parecido a un diccionario/mapa (campo → valor). El catálogo vigente se guarda así, con el ID de cada entidad como campo.

**Namespace / ACL:** un prefijo de claves (`catedra:sync:*`, `alumnos:{groupId}:*`) sobre el que tenés permisos de lectura y/o escritura específicos. No podés leer ni escribir fuera de lo que te dieron.

**TTL (Time To Live):** tiempo de vida de una clave o de un recurso antes de expirar solo. Los *holds* de turnos, por ejemplo, tienen TTL — aunque vos lo guardes localmente, si pasó el `expiresAt`, ya no vale en el servidor central.

## Reservas

**Hold:** un bloqueo temporal sobre un turno mientras el usuario completa el proceso de reserva, para que dos personas no reserven el mismo horario a la vez.

**Idempotencia:** la propiedad de que repetir una operación no cambia el resultado más allá de la primera vez. Es clave en dos lugares: procesar el mismo evento Kafka dos veces, y reintentar una operación REST después de un timeout sin saber si la primera vez llegó a aplicarse.

**Máquina de estados:** el conjunto de estados posibles de una reserva (`PHONE_PENDING`, `CONFIRMED`, `CANCELLED`, etc.) y las transiciones válidas entre ellos. La tenés que diseñar vos para tu servicio de turnos, evitando que un evento tardío "retroceda" un estado que ya es final.

## Documentación de decisiones

**ADR (Architecture Decision Record):** un documento corto por cada decisión de diseño importante (qué elegiste, qué alternativas consideraste, por qué). No es un invento de este proyecto — es una práctica estándar de la industria para dejar registro de *por qué* el código quedó como quedó. Ver `ADR-template.md`.

**`problem+json`:** un formato estándar (RFC 7807) para representar errores HTTP con `type`, `title`, `status`, `detail`, `code`. El contrato de la cátedra lo usa; podés adoptarlo también para los errores entre tus dos servicios.