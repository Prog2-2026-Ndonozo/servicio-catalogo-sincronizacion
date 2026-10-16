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

Java 25 · Spring Boot 4.1 · Maven · PostgreSQL · Spring Data JPA · Kafka · Redis · springdoc (OpenAPI) ·
Lombok · ArchUnit.

## Requisitos

- JDK 25
- Docker y Docker Compose (para PostgreSQL local)
- Cuenta técnica registrada en la cátedra y su JWT (I §5). Se guarda en `cuenta_tecnica.json`, que **no** se versiona.

## Ejecutar

```bash
cp .env.example .env          # una sola vez; completá DB_PASSWORD (el .env no se versiona)
docker compose up -d          # PostgreSQL local con volumen persistente
set -a; source .env; set +a   # carga las variables del .env en esta terminal
./mvnw spring-boot:run        # ejecución local

./mvnw test                   # tests, incluidas las reglas de arquitectura
```

Docker Compose lee el `.env` por sí solo, pero Spring Boot **no**: por eso el paso `source .env`. Sin él la app
usa los valores por defecto de `application.yaml` y puede no coincidir con la base que creó Compose.

### Base de datos local

Variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` (ver `.env.example`). Las de Redis, Kafka
y la API de la cátedra se agregan en los issues que las usan. Ningún valor real se sube al repositorio.

- Los datos viven en el volumen `postgres_data` y sobreviven a `docker compose down` y `up`.
- PostgreSQL toma el usuario y la contraseña **solo al crear el volumen por primera vez**. Cambiar el `.env`
  después no los modifica.
- Para empezar de cero: `docker compose down -v` (**borra todos los datos**).

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
