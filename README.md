# Servicio de Catálogo y Sincronización

Backend del proyecto integrador 2026 (Programación 2). Mantiene la copia local del catálogo de categorías,
profesionales y horarios semanales, la sincroniza con el servicio central de la cátedra (snapshot completo e
incremental por Kafka + Redis) y resuelve las búsquedas de profesionales únicamente con datos locales.

Es uno de los dos backends del proyecto; el otro es `servicio-turnos-reservas`. Solo se comunican por HTTP con
JWT, sin acceso cruzado a bases de datos.

## Fuente única de la verdad

Todo el contrato de este servicio se deriva de dos documentos de la cátedra, que prevalecen sobre cualquier
otro texto de este repo:

- [`PROJECT_STATEMENT-v1.md`](../documentacion/PROJECT_STATEMENT-v1.md) — enunciado y restricciones (**E**).
- [`INTEGRATION_REFERENCE-v2.md`](../documentacion/INTEGRATION_REFERENCE-v2.md) — contratos REST, Redis y Kafka (**I**).

[`docs/fuentes.md`](docs/fuentes.md) indica qué sección de cada uno aplica a este servicio.

## Stack

Java 25 · Spring Boot 4.1 · Maven · PostgreSQL · Flyway · Spring Data JPA · Kafka · Redis · springdoc (OpenAPI) ·
Lombok · ArchUnit.

## Requisitos

- JDK 25
- Docker y Docker Compose (para PostgreSQL local)
- Cuenta técnica registrada en la cátedra y su JWT (I §5). Va en el `.env` como `CATEDRA_JWT`, que **no** se versiona.

## Ejecutar

```bash
cp .env.example .env          # una sola vez; completá DB_PASSWORD y las CATEDRA_* (el .env no se versiona)
docker compose up -d          # PostgreSQL local con volumen persistente
./mvnw spring-boot:run        # ejecución local

./mvnw test                   # tests, incluidas las reglas de arquitectura
```

El mismo `.env` lo leen Docker Compose y la aplicación (`spring.config.import` en `application.yaml`), así que las
credenciales se definen en un solo lugar. Si una variable también existe en el entorno del sistema, esa gana.
`DB_PASSWORD` no tiene valor por defecto: sin ella, ni Compose ni la app arrancan.

### Base de datos local

Variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` (ver `.env.example`). Las de Redis y Kafka
se agregan en los issues que las usan. Ningún valor real se sube al repositorio.

- Los datos viven en el volumen `postgres_data` y sobreviven a `docker compose down` y `up`.
- PostgreSQL toma el usuario y la contraseña **solo al crear el volumen por primera vez**. Cambiar el `.env`
  después no los modifica.
- Para empezar de cero: `docker compose down -v` (**borra todos los datos**).

### API de la cátedra

Variables: `CATEDRA_BASE_URL` (URL base del servicio Java de la cátedra) y `CATEDRA_JWT` (JWT de la cuenta técnica).
Son obligatorias: sin ellas la aplicación no arranca. El JWT nunca se loguea ni se imprime (ADR-0020).

Tiempos de espera de los clientes HTTP, en `application.yaml` (`spring.http.clients.*`, ADR-0019): conexión 5 s y
lectura 30 s. Si la cátedra es más lenta, subí `read-timeout`. Cuando la cátedra no responde, devuelve un error o
manda un cuerpo ilegible, el servicio lo trata como "cátedra no disponible" y no modifica el catálogo local (ADR-0022).

### Migraciones

El esquema lo crea Flyway con SQL plano, en `src/main/resources/db/migration` (`V1__crear_esquema_inicial.sql`,
`V2__...sql`, etc.). Se aplican solas al arrancar la aplicación. Hibernate corre en `ddl-auto: validate`: no crea
ni modifica nada, solo comprueba que las entidades coincidan con las tablas.

- Una migración ya aplicada **no se edita**: Flyway se niega a arrancar si cambia su contenido. Un cambio de
  esquema se hace con una migración nueva.
- Las claves foráneas son `DEFERRABLE INITIALLY DEFERRED`: la base las verifica al confirmar la transacción, por
  lo que dentro de una misma transacción el orden de inserción no importa (ver `docs/DECISIONES.md`).
- Si te equivocás en una migración durante el desarrollo y la base no tiene datos que importen:
  `docker compose down -v` y volver a levantar (**borra todos los datos**).

## Arquitectura

Hexagonal, un único módulo `catalog` con tres capas (`domain`, `application`, `infrastructure`) más `config/`.
Las reglas de dependencia están automatizadas con ArchUnit (`ArchitectureTest`).
Detalle: [`docs/arquitecturaHexagonalServicioCatalogo.md`](docs/arquitecturaHexagonalServicioCatalogo.md).

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/fuentes.md`](docs/fuentes.md) | Índice de las fuentes de verdad por tema |
| [`docs/arquitecturaHexagonalServicioCatalogo.md`](docs/arquitecturaHexagonalServicioCatalogo.md) | Paquetes, puertos, fachadas, reglas ArchUnit |
| [`docs/BacklogServicioCatalogo.md`](docs/BacklogServicioCatalogo.md) | Issues, milestones y definición de terminado |
| [`docs/DECISIONES.md`](docs/DECISIONES.md) | Decisiones tomadas y pendientes |
| [`docs/glosario.md`](docs/glosario.md) | Vocabulario del proyecto |
| [`AGENTS.md`](AGENTS.md) | Contexto y reglas para agentes de IA |

## Convenciones

Un issue = una rama = un PR. Commits con [Conventional Commits](https://www.conventionalcommits.org/).
