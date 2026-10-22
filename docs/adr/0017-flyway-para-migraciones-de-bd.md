# ADR-0017: Flyway para las migraciones de la base de datos

Fecha: 2026-10-16
Estado: aceptada

## Contexto
El enunciado exige que cada servicio sea dueño exclusivo de sus datos y
"administre sus propias migraciones" (E §3). La persistencia principal es
PostgreSQL, un motor de servidor (E §3). El issue #4 pide el esquema inicial
con una herramienta de migraciones (Flyway o Liquibase).

## Opciones consideradas
1. **Flyway.** Migraciones en SQL plano, ordenadas por número de versión.
   + Se lee exactamente qué se ejecuta; no hay un formato nuevo que aprender.
   + Permite escribir en el SQL las restricciones CHECK de las validaciones
     del ADR-0014.
   − El SQL es propio de PostgreSQL: cambiar de motor obligaría a reescribirlo.
2. **Liquibase.** Cambios descritos en XML/YAML/JSON o SQL con metadatos.
   + Independiente del motor de base de datos.
   − Formato adicional que aprender y mantener; no se necesita portabilidad.
3. **Dejar que Hibernate cree las tablas (`ddl-auto: update`).**
   − No es versionado ni reproducible; no cumple "administrar sus migraciones".

## Decisión
Opción 1. Flyway, con migraciones SQL en `src/main/resources/db/migration`
(`V1__...sql`, `V2__...sql`). Hibernate queda en `ddl-auto: validate`: solo
comprueba que las entidades coincidan con el esquema que creó Flyway.

## Justificación
El motor ya está fijado en PostgreSQL, así que la portabilidad de Liquibase no
aporta, y el SQL plano es la forma más directa de leer y justificar el esquema.
`validate` mantiene a Flyway como única fuente del esquema.

## Consecuencias
- Dependencias nuevas: `spring-boot-starter-flyway` y `flyway-database-postgresql`
  (sin versión, la gestiona el BOM de Spring Boot).
- Una migración ya aplicada no se edita nunca: un cambio de esquema es una
  migración nueva. Flyway rechaza arrancar si cambia el contenido de una ya aplicada.
- Las migraciones se ejecutan al iniciar la aplicación, contra la base del `.env`.
- Los tests que levanten el contexto necesitan una base PostgreSQL real
  (H2 no está permitido).