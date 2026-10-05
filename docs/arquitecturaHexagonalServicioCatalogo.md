# Arquitectura hexagonal — servicio de catálogo y sincronización

> Aplicación **a este servicio** del catálogo hexagonal `hexagonal-catalogo`
> (`../../hexagonal/.agents/skills/hexagonal-catalogo/`). Acá solo está lo propio de catálogo: paquetes,
> puertos, fachadas y reglas ArchUnit. Las convenciones generales viven en el skill y **ganan** ante
> cualquier diferencia. Los contratos externos están en E e I (`fuentes.md`).

## Dónde está lo genérico (no se repite acá)

| Necesito saber | Skill (`references/`) |
|---|---|
| Qué hace cada paquete, qué puede importar, dónde NO va una clase | `estructura-paquetes.md` |
| Cómo escribir un caso de uso (Query/Command, fachada, excepciones, `@Transactional`) | `casos-de-uso.md` |
| Controller, DTOs, mappers, entidad JPA, adaptador nuevo (Kafka/Redis) | `adaptadores.md`, `adaptadores-nuevos.md` |
| Qué testear en cada capa | `testing.md`, `testing-adaptadores.md` |

## Las tres capas

```
infrastructure  ─────►  application  ─────►  domain
```

- `domain/`: modelo y puertos. Solo `java.*` y Lombok. Cero Spring, JPA, Jakarta, Jackson.
- `application/`: casos de uso (`@Component`), fachadas (`@Service`), excepciones. No conoce `infrastructure/`.
- `infrastructure/`: REST, JPA, Kafka, Redis, cliente HTTP. Reemplazable sin tocar lo de adentro.
- `config/` (fuera de `catalog/`): composition root; cablea clientes Redis, `WebClient`, seguridad.

Un solo módulo (`catalog`): sincronización y búsqueda comparten modelo y puerto de repositorio; partirlas
obligaría a duplicar conceptos o a crear un `shared`. Si hiciera falta separarlas, el módulo es un
directorio nuevo y el resto no se toca.

## Estructura de paquetes

```
servicio-catalogo/
└── src/main/java/com/ndonozo/serviciocatalogosincronizacion/
    ├── ServicioCatalogoSincronizacionApplication.java
    ├── catalog/
    │   ├── domain/                              ← PURO: sin Spring, sin JPA, sin Jakarta
    │   │   ├── model/
    │   │   │   ├── Professional.java
    │   │   │   ├── ProfessionalCategory.java
    │   │   │   ├── WeeklySchedule.java
    │   │   │   └── CatalogVersion.java
    │   │   └── ports/
    │   │       ├── in/                          ← un puerto por caso de uso
    │   │       │   ├── SearchProfessionalsUseCase.java
    │   │       │   ├── ApplyFullSnapshotUseCase.java
    │   │       │   ├── ApplyIncrementalChangeUseCase.java
    │   │       │   └── GetSyncStatusUseCase.java
    │   │       └── out/                         ← lo que el negocio necesita del exterior
    │   │           ├── CatalogRepositoryPort.java
    │   │           ├── CatedraSnapshotPort.java
    │   │           ├── CatedraCatalogReadPort.java
    │   │           └── SyncDiagnosticsPort.java
    │   ├── application/                         ←Spring sí, infrastructure no
    │   │   ├── exception/
    │   │   │   └── ProfessionalNotFoundException.java
    │   │   ├── service/                         ← fachadas @Service, único entry point de drivers
    │   │   │   ├── ProfessionalSearchService.java
    │   │   │   └── CatalogSyncService.java
    │   │   └── usecases/                        ← lógica de aplicación, @Component
    │   │       ├── SearchProfessionalsUseCaseImpl.java
    │   │       ├── ApplyFullSnapshotUseCaseImpl.java
    │   │       ├── ApplyIncrementalChangeUseCaseImpl.java
    │   │       └── GetSyncStatusUseCaseImpl.java
    │   └── infrastructure/
    │       ├── persistence/                     ← PostgreSQL
    │       │   ├── entity/                      CategoryEntity, ProfessionalEntity, WeeklyScheduleEntity
    │       │   ├── repository/                  JpaCatalogRepository
    │       │   ├── mapper/                      ProfessionalMapper, ...
    │       │   └── adapter/                     JpaCatalogRepositoryAdapter → CatalogRepositoryPort
    │       ├── catedrarest/                     ← GET /api/synchronization/snapshot
    │       │   ├── client/                      CatedraRestClient
    │       │   ├── dto/                         CatedraSnapshotResponse
    │       │   ├── mapper/                      SnapshotMapper
    │       │   └── adapter/                     CatedraSnapshotRestAdapter → CatedraSnapshotPort
    │       ├── catedraredis/                    ← catedra:sync:*
    │       │   ├── client/                      CatedraRedisClient
    │       │   ├── mapper/                      CatalogMetadataMapper
    │       │   └── adapter/                     CatedraRedisAdapter → CatedraCatalogReadPort
    │       ├── messaging/                       ← Kafka catedra.catalog.{groupId}
    │       │   ├── dto/                         CatalogUpdatedMessage
    │       │   ├── mapper/                      CatalogUpdatedMapper
    │       │   └── CatalogUpdatedListener.java  → dispara ApplyIncrementalChangeUseCase
    │       └── web/                             ← endpoints propios
    │           ├── controller/                  ProfessionalSearchController, SyncStatusController
    │           ├── dto/                         SearchProfessionalsRequest, ProfessionalResponse
    │           └── mapper/                      ProfessionalDtoMapper
    └── config/
        ├── SecurityConfig.java     (JWT: usuario final en REST propio, técnico inter-servicios y hacia cátedra)
        ├── KafkaConfig.java
        ├── RedisConfig.java
        └── WebClientConfig.java
```

Las carpetas se crean recién hasta el nivel que nombra una decisión. Las hojas
(`entity/`, `repository/`, `mapper/`, `adapter/`, `client/`, `dto/`, `controller/`)
nacen con el primer archivo de esa tecnología, en la issue que la toque.

## Las siete reglas, y quién las verifica

Viven en `src/test/java/.../ArchitectureRules.java` y corren en `ArchitectureTest`.
`ArchitectureRulesFixtureTest` comprueba con clases de `src/test/java/com/ndonozo/arquitecturafixtures/`
que cada regla **detecta** una violación real (los fixtures no son código de producción).

| Regla | Qué prohíbe |
|---|---|
| `MODULO_SOLO_TIENE_TRES_CAPAS` | una clase dentro de `catalog/` fuera de `domain/`, `application/` o `infrastructure/` |
| `DOMINIO_ES_PURO` | `domain/` → `application/`, `infrastructure/`, `config/`, Spring, Jakarta, Jackson |
| `APLICACION_NO_CONOCE_INFRAESTRUCTURA` | `application/` → `infrastructure/`, `config/`, Spring Web/Data/Kafka, `jakarta.persistence`, Jackson |
| `WEB_NO_VE_PERSISTENCIA_NI_PUERTOS_DE_SALIDA` | `infrastructure/web/` → `domain/ports/out/` o `infrastructure/persistence/` |
| `PUERTOS_DE_ENTRADA_SOLO_SE_IMPLEMENTAN_EN_APLICACION` | implementar un puerto de entrada desde un lugar que no sea `application/` |
| `SERVICE_SOLO_EN_LAS_FACHADAS` | `@Service` fuera de `application/service/` (los casos de uso son `@Component`) |
| `SIN_INYECCION_POR_CAMPO` | `@Autowired` en un campo (siempre constructor) |

Corren con `allowEmptyShould(true)`: los paquetes están vacíos durante varias issues; la guarda la
garantizan los fixtures.

## Puertos de entrada — quién los llama y con qué JWT

Un driver externo (controller, listener de Kafka) **nunca** inyecta un puerto de entrada:
entra por la fachada de `application/service/`. Eso concentra en un lugar el `orElseThrow`
de cada caso de uso y deja un único punto de entrada por responsabilidad.

| Puerto in | Fachada | Adaptador in | Consumidor | JWT esperado |
|---|---|---|---|---|
| `SearchProfessionalsUseCase` | `ProfessionalSearchService` | REST `/api/professionals/search` | App KMP | JWT de usuario final |
| `SearchProfessionalsUseCase` (mismo caso de uso, distinto endpoint o header) | `ProfessionalSearchService` | REST `/api/internal/professionals/search` | servicio-turnos | JWT técnico inter-servicios |
| `ApplyIncrementalChangeUseCase` | `CatalogSyncService` | `messaging/CatalogUpdatedListener` | Evento `CatalogUpdated` de cátedra | N/A (no es HTTP) |
| `ApplyFullSnapshotUseCase` | `CatalogSyncService` | disparado desde `CatalogSyncService` ante base vacía o discontinuidad | Interno | N/A |
| `GetSyncStatusUseCase` | `CatalogSyncService` | REST `/api/sync/status` | Observabilidad / debugging propio | JWT técnico inter-servicios (opcional exponerlo también a un admin en KMP) |

Decisión pendiente: si KMP nunca le pega directo a catálogo, se elimina la fila de KMP y turnos queda
como único consumidor de la búsqueda (ver `DECISIONES.md`).

## Puertos de salida — a qué le hablan y quién los implementa

| Puerto out | Fachada del adapter | Para qué |
|---|---|---|
| `CatalogRepositoryPort` | `infrastructure/persistence/JpaCatalogRepositoryAdapter` | Persistencia propia de categorías, profesionales, horarios y versión local |
| `CatedraSnapshotPort` | `infrastructure/catedrarest/CatedraSnapshotRestAdapter` | `GET /api/synchronization/snapshot` |
| `CatedraCatalogReadPort` | `infrastructure/catedraredis/CatedraRedisAdapter` | Metadata, Hashes de entidades y `changes:{version}` de Redis |
| `SyncDiagnosticsPort` | **sin definir** (`DECISIONES.md`) | Guardar último resultado de sync para `GetSyncStatusUseCase` |

`SyncDiagnosticsPort` no se define hasta que la issue #7 fije la máquina de estados de la sincronización.

## Consecuencias propias de este servicio

- **Aislamiento de datos (E §3):** `servicio-turnos` solo accede por el REST de `infrastructure/web/controller`;
  nunca a `domain/` ni a `infrastructure/persistence/`.
- **Idempotencia y discontinuidad:** la deduplicación por `eventId` (I §15.1) y el chequeo contra
  `oldestAvailableVersion` (I §14.2, §18.2) viven en el caso de uso, no en el listener ni en el adapter; así se
  testean sin Kafka ni Redis.
- **Transacción de sync:** `@Transactional` en `ApplyFullSnapshotUseCaseImpl`; la versión local solo avanza si
  las tres colecciones quedaron aplicadas (E §6.1).
- **Comandos devuelven resultado:** `ApplyFullSnapshotUseCase` devuelve
  `SyncResult(appliedVersion, categoriesCount, professionalsCount, schedulesCount)`.
- **Drivers por la fachada:** `CatalogUpdatedListener` inyecta `CatalogSyncService`, nunca
  `ApplyIncrementalChangeUseCase`.
