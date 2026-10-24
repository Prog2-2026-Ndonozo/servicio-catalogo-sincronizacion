# ADR-0021: CatalogSnapshot como resultado del puerto de la cátedra

Fecha: 2026-10-24
Estado: aceptada

## Contexto
`CatedraSnapshotPort` debe devolver al núcleo lo que la cátedra publicó en un momento dado (I §7): una versión
y las tres colecciones del catálogo. El núcleo no debe conocer HTTP ni JSON. El ADR-0010 define
`CatalogVersion` como la versión **local aplicada**.

## Opciones consideradas
1. **Devolver el DTO de la cátedra.** Filtra un detalle de infraestructura al dominio.
2. **Un `CatalogSnapshot` del dominio que guarda un `CatalogVersion`.** Mezcla "versión publicada" con
   "versión aplicada localmente", que son conceptos distintos.
3. **Un `CatalogSnapshot` del dominio con `long snapshotVersion` y tres listas.**

## Decisión
Opción 3, en `catalog/domain/model/`: objeto inmutable (ADR-0015) con `snapshotVersion` (`long` ≥ 0) y las
listas `professionals`, `categories` y `weeklySchedules`.
- Las listas no pueden ser `null`; una lista vacía es válida (un catálogo sin horarios es un estado legítimo).
- Se guardan con `List.copyOf`: copia defensiva e inmutable.
- No incluye `generatedAt`: ningún paso de la sincronización lo usa.
- El caso de uso crea el `CatalogVersion` a partir de `snapshotVersion` al aplicar el snapshot.
- La validación lanza `InvalidCatalogDataException` (ADR-0016).

## Justificación
El puerto habla en el vocabulario del dominio. Separar "versión publicada" de "versión aplicada" mantiene
fiel el significado de `CatalogVersion` (ADR-0010) y deja claro el momento en que la versión local avanza: solo
cuando el caso de uso termina bien.

## Consecuencias
- El adaptador REST (`SnapshotMapper`) traduce el DTO a `CatalogSnapshot`, incluida la conversión de
  `description` nula a `""` (ADR-0014).
- Un elemento `null` dentro de una lista haría fallar `List.copyOf` con `NullPointerException`; no se
  contempla porque los mappers nunca los producen.
- El caso de uso se puede probar con un `CatedraSnapshotPort` simulado, sin red.
