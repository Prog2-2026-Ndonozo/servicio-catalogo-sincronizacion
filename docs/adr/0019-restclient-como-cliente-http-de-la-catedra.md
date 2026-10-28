# ADR-0019: RestClient como cliente HTTP hacia la cátedra

Fecha: 2026-10-24
Estado: aceptada

## Contexto
La sincronización completa obtiene el catálogo con `GET /api/synchronization/snapshot` (I §7), autenticado con el
JWT técnico (I §5). Hace falta un cliente HTTP en `infrastructure/catedrarest/`. El resto del servicio es
bloqueante: acceso a datos con JPA/JDBC y casos de uso transaccionales (ADR-0018).

## Opciones consideradas
1. **`WebClient`** (Spring WebFlux).
   + Soporta llamadas reactivas y no bloqueantes.
   − Obliga a sumar WebFlux. En un servicio bloqueante se termina llamando a `.block()`, con la complejidad
     reactiva y sin su beneficio.
2. **`RestClient`** (Spring Framework 6.1+).
   + API fluida y síncrona, hecha para código bloqueante.
   + Boot publica un `RestClient.Builder` ya configurado (Jackson, convertidores), que respeta la
     configuración de la aplicación.
   + Se prueba con `MockRestServiceServer`, sin levantar un servidor.
3. **`RestTemplate`**.
   − Está en modo mantenimiento; `RestClient` lo reemplaza.
4. **`HttpClient` de Java a mano.**
   − Obliga a resolver a mano la serialización, los errores y los headers.

## Decisión
Opción 2. El cliente es un `RestClient` construido en `config/RestClientConfig` a partir del
`RestClient.Builder` de Boot, con `baseUrl` y el header `Authorization`. `CatedraRestClient` y el adaptador lo
reciben ya configurado.

## Justificación
El servicio es bloqueante de punta a punta, así que un cliente síncrono es el que mejor encaja. `RestClient`
evita incorporar una pila reactiva que nada más usaría y mantiene el código del adaptador lineal y fácil de
testear.

## Consecuencias
- No se agrega `spring-boot-starter-webflux`.
- Se inyecta el `RestClient.Builder` de Boot y **no** el estático `RestClient.builder()`, para que se aplique la
  configuración de Jackson de Boot (por ejemplo, ignorar propiedades desconocidas del JSON).
- El adaptador se prueba con `MockRestServiceServer`; no hacen falta tests contra la red de la cátedra.
- `docs/arquitecturaHexagonalServicioCatalogo.md` nombra `RestClientConfig` (antes `WebClientConfig`).
- Los clientes de Redis y Kafka no se ven afectados.
- **Dependencia:** Spring Boot 4 separó la auto-configuración del cliente HTTP en módulos propios. Hace falta
  `spring-boot-starter-restclient` en el `pom.xml`; sin él no existe el bean `RestClient.Builder` y la
  aplicación no arranca.
- **Timeouts:** el cliente no tiene tiempos por defecto, así que se configuran en `application.yaml` con
  `spring.http.clients.connect-timeout` (5 s) y `spring.http.clients.read-timeout` (30 s). No se usan las
  propiedades `spring.http.client.*`, deprecadas desde Spring Boot 4.0. Se aplican a todo cliente HTTP que
  arme Boot. `RestClientConfigTimeoutTest` comprueba que la propiedad realmente corta una respuesta lenta.
