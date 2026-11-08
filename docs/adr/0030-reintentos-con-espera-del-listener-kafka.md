# ADR-0030: Reintentos con espera exponencial en el listener de Kafka

Fecha: 2026-11-08
Estado: aceptada

## Contexto
El listener de `CatalogUpdated` (ADR-0026) deja propagar los fallos operativos para que Spring Kafka los reintente.
Sin configuración propia rige el valor por defecto: 10 intentos seguidos, **sin espera** entre ellos, y después el
mensaje se salta con un error. En la primera prueba con eventos reales (credenciales de Redis mal configuradas)
los 54 eventos agotaron los intentos en segundos y se perdieron. Un corte transitorio de Redis, de PostgreSQL o de
la red habría tenido el mismo efecto.

El contrato pide que los reintentos sean **limitados y observables** (I §18.4).

## Opciones consideradas
1. **Dejar el valor por defecto.** Limitado, pero sin espera no sirve ante un corte transitorio, y el log no dice
   qué versión se descartó.
2. **Espera fija** (por ejemplo 5 s, 5 veces). Sencillo, pero espera lo mismo ante un fallo breve que ante uno
   largo.
3. **Espera exponencial con tope y cantidad máxima de reintentos.** Reintenta rápido al principio, se espacia
   después y termina.
4. **Reintentos de larga duración (minutos).** Cubriría cortes mayores, pero bloquea la partición y exige subir
   `max.poll.interval.ms`, con riesgo de que Kafka expulse al consumidor del grupo.

## Decisión
Opción 3. `KafkaErrorHandlingConfig` (infraestructura/messaging, `@Configuration`) define un bean
`CommonErrorHandler` que Spring Boot aplica solo al contenedor del listener:

- `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries(5)`: intervalo inicial de 1 s, multiplicador 2 y
  tope de 10 s. Esperas de 1, 2, 4, 8 y 10 s: unos 25 s en total y 6 intentos contando el primero.
- **Observabilidad:** cada fallo intermedio se registra como `WARN` con `offset`, número de intento y la excepción.
  Al agotarse los reintentos se registra un `ERROR` con `partition`, `offset` y `key`; la key de `CatalogUpdated` es
  `newVersion` en texto (I §15.1), así que el log dice qué versión se descartó. No se registra el payload.
- Los valores son constantes en la clase. No se exponen como propiedades por ahora.
- La clasificación de excepciones queda en el valor por defecto de Spring Kafka. Un mensaje defectuoso ya se
  descarta en el listener sin llegar acá (ADR-0026).

## Consecuencias
- El total de ~25 s queda muy por debajo de `max.poll.interval.ms` (5 min por defecto), por lo que no hay riesgo de
  expulsión del grupo. Subir las esperas por encima de ese límite exigiría cambiar esa propiedad y se desaconseja.
- Mientras se reintenta, el hilo del listener queda ocupado y los eventos siguientes de la partición esperan.
- Un corte de más de ~25 s sigue perdiendo el evento. Se amortigua por diseño: el caso de uso recorre desde la
  versión local + 1 hasta la del evento (ADR-0028), de modo que el **evento siguiente repara el hueco**. Eso
  depende de que la cátedra publique otro evento.
- **Pendiente, fuera de alcance:** al arrancar solo se pide snapshot si la base está vacía; no se compara la versión
  local con `currentVersion` de Redis para ponerse al día sin esperar un evento.
- Los WARN muestran la excepción envuelta por Spring Kafka (`ListenerExecutionFailedException`), no su causa. Mejora
  posible: registrar la causa directa.
- Tests: la clase está anotada como `@Configuration` y el bean existe como `DefaultErrorHandler`. Los intervalos no
  se verifican de forma automática (el manejador no los expone); se comprueban mirando el espaciado de los WARN.
