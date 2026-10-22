# ADR-0010: Modelo de dominio base del servicio de catálogo

Fecha: 2026-10-06
Estado: aceptada (los conceptos y sus datos); propuesta (la referencia por ID, ver Decisión §2)

## Contexto
El issue #3 pide el modelo de dominio base. El servicio mantiene una copia local del catálogo de la cátedra
(E §4.1) y no puede consultarla en cada búsqueda (E §6.2). El contrato publica el catálogo como tres colecciones
más una versión (I §7, I §14.4), y E §4.1 exige filtrar por categoría, nombre, estado habilitado y disponibilidad.

## Opciones consideradas
1. **Un concepto de dominio por cada colección del contrato, más la versión local.**
   + Coincide con cómo se publica y se sincroniza el catálogo (por ID, entidad a entidad).
   − Hoy los atributos coinciden con los del JSON de la cátedra; hay que mantener separadas las
     representaciones (JSON, dominio, tabla) aunque se parezcan.
2. **Usar directamente las estructuras del contrato (JSON de Redis) como modelo.**
   + Menos clases.
   − Acopla el dominio al formato de la cátedra: un campo nuevo o un cambio de tipo se filtra al dominio.
     Viola la regla de que `domain/` no conoce el mundo exterior.

## Decisión
Opción 1. Cuatro conceptos en `catalog/domain/model/`:

| Concepto | Datos | Relación |
|---|---|---|
| `ProfessionalCategory` | id, name, description, enabled, createdAt, updatedAt | — |
| `Professional` | id, categoryId, firstName, lastName, enabled, createdAt, updatedAt | categoría |
| `WeeklySchedule` | id, professionalId, dayOfWeek, startTime, endTime, slotDurationMinutes, enabled, createdAt, updatedAt | profesional |
| `CatalogVersion` | el número de versión local aplicada | — |

1. `enabled` es el estado de **baja lógica** de cada registro (I §14.4). No es "disponibilidad": ese filtro
   (E §4.1) es otro concepto y no se define en este ADR.
2. **Propuesta, todavía no cerrada:** las entidades se relacionan por ID (`categoryId`, `professionalId`) y no
   por referencia a objeto, de modo que el nombre de la categoría se obtiene buscándola por su ID en la copia
   local. Se confirma o ajusta al implementar la sincronización.
3. `CatalogVersion` representa la versión **local aplicada**, no la publicada por la cátedra. Solo avanza
   cuando todos los cambios de esa versión se aplicaron (E §6.2).

## Justificación
Sigue la forma en que la cátedra publica y sincroniza el catálogo y mantiene el dominio independiente del
formato externo (el JSON se traduce en el adaptador, la tabla en `infrastructure/persistence`). Cada concepto
puede cambiar por una razón distinta sin arrastrar a los otros.

## Consecuencias
- Cada concepto es una clase de dominio pura (solo `java.*` y Lombok); la regla `DOMINIO_ES_PURO` lo verifica.
- Los mappers desde el JSON de la cátedra y hacia JPA se escriben en `infrastructure/`, en issues posteriores.
- **Huecos del contrato, sin resolver:** a qué se refiere "nombre" en el filtro de E §4.1 (profesional,
  categoría o ambos); si una categoría deshabilitada afecta a sus profesionales.
- Las validaciones de cada concepto (por ejemplo `startTime` anterior a `endTime`) y el filtro de
  "disponibilidad" se deciden aparte.
