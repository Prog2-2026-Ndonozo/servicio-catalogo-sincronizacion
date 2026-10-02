# Servicio de Catálogo y Sincronización

## Qué es este repo
Uno de los dos backends del proyecto integrador (sistema distribuido de turnos, cátedra 2026). Este servicio es el dueño exclusivo de la copia local del catálogo (categorías, profesionales y horarios semanales) y de la versión aplicada. Se encarga de sincronizarla con el servicio central de la cátedra (snapshot completo e incremental) y de resolver las búsquedas de profesionales siempre contra datos locales. Se comunica con el repo hermano `servicio-turnos-reservas` únicamente vía HTTP con JWT — nunca accede a su base de datos, y viceversa.

Documentación de referencia:
- `docs/BacklogServicioCatalogo` — issues y milestones
- `docs/arquitecturaHexagonalServicioCatalogo` — puertos y adaptadores
- `docs/glosario` — vocabulario del proyecto

## Stack
- Java + Spring Boot
- Base de datos: PostgreSQL, propia y exclusiva de este servicio (prohibido H2/SQLite/embebidas como persistencia principal)
- Cliente Kafka: consumer del topic `catedra.catalog.{groupId}`
- Cliente Redis: lectura de `catedra:sync:*` (via `CatedraCatalogReadPort`); namespace propio `alumnos:{groupId}:*` solo para estado auxiliar
- OpenAPI/Swagger: springdoc

## Comandos comunes
```
docker compose up -d        # levanta este servicio + su base de datos
./mvnw test                 # corre los tests
./mvnw spring-boot:run      # corre local sin docker
```

## Responsabilidades de este servicio (no invadir las del otro repo)
- Almacenar la copia local de categorías, profesionales y horarios semanales.
- Conservar la versión de catálogo aplicada.
- Ejecutar sincronización completa (snapshot REST `/api/synchronization/snapshot`) e incremental (Kafka `CatalogUpdated` + Redis `catedra:sync:*`).
- Detectar discontinuidades y reconstruir la copia local.
- Buscar y filtrar profesionales con datos exclusivamente locales (nunca consultar a la cátedra por cada búsqueda).
- Informar el estado y los errores de sincronización.
- **No** construir disponibilidad, ni holds, ni reservas: eso es responsabilidad exclusiva de `servicio-turnos-reservas`.

## Reglas para el agente
- Nunca inventar campos o estados que no estén en `docs/` — si falta un dato del contrato, preguntar o señalar el gap en vez de asumir.
- Todo cambio en la máquina de sincronización (versiones, idempotencia, reconstrucción) debe quedar reflejado en `DECISIONES.md` con su justificación.
- No sugerir H2/SQLite ni bases embebidas como persistencia principal (prohibido por el enunciado).
- El JWT técnico de cátedra jamás debe loguearse, versionarse ni exponerse. Está en `cuenta_tecnica.json` (gitignoreado).
- Antes de tocar manejo de duplicados/reintentos/discontinuidades, releer la sección 6 y 8 del enunciado y `docs/glosario`.
- PROHIBIDO realizar commit, crear ramas, pushear al repositorio sin confirmación previa
- No cambiar tecnologías ya propuestas
- Recordarme a mi (Nehuen) que cada vez que consideremos que tenemos que hcaer el commit hacerlo segun el estandar de conventional commits

## Decisiones de diseño ya tomadas
Ver `DECISIONES.md`. Resumen rápido (actualizar a medida que avances):
- Arquitectura hexagonal: el dominio no conoce Spring/JPA/Kafka/Redis (ver `docs/arquitecturaHexagonalServicioCatalogo`).
- Estrategia de idempotencia: deduplicar por `eventId` de Kafka (completar).
- Motor de base de datos: PostgreSQL (completar)