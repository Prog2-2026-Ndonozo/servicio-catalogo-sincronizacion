# ADR-0016: Excepción de validación del modelo, ubicada en `domain/` (desvío del skill)

Fecha: 2026-10-06
Estado: aceptada

## Contexto
Con el modelo inmutable que se valida al construirse (ADR-0015), es el propio modelo, en `domain/`, quien lanza
un error cuando un dato no cumple las reglas del ADR-0014. El skill `hexagonal-catalogo` (`casos-de-uso.md` §7,
regla 1) ubica las excepciones de negocio en `application/exception/` y no en `domain/`. Pero `domain/` no puede
importar `application/` (regla `DOMINIO_ES_PURO`, verificada por ArchUnit), así que el modelo no puede lanzar una
excepción definida allí. Además, la pregunta abierta del ADR-0014 (qué hacer con un dato inválido publicado por
la cátedra) solo se puede resolver si el error tiene un tipo propio que el caso de uso pueda distinguir.

## Opciones consideradas
1. **Excepción estándar de Java** (`IllegalArgumentException`).
   + Cumple el skill y `DOMINIO_ES_PURO`; no agrega clases.
   − No se distingue de otros argumentos inválidos o de errores de programación; el caso de uso no puede decidir
     con precisión qué hacer ante un dato inválido de la cátedra.
2. **Excepción propia en `domain/`.**
   + El modelo la puede lanzar sin romper la dependencia; tiene tipo propio, así que el caso de uso la captura
     específicamente.
   − Se aparta de la regla 1 del skill (ubicación) y agrega una clase y una carpeta `domain/exception/`.

## Decisión
Opción 2. Una excepción de validación del modelo (nombre provisorio `InvalidCatalogDataException`) en
`catalog/domain/exception/`. Extiende `RuntimeException`, no menciona HTTP y su mensaje, en inglés, describe la
regla incumplida. La lanza el modelo al construirse con datos inválidos.

Las excepciones que lanzan los casos de uso o la fachada (por ejemplo `ProfessionalNotFoundException`) se
mantienen en `application/exception/`, como indica el skill.

## Justificación
La ubicación depende de quién lance la excepción: lo que lanza el modelo debe vivir donde el modelo puede verlo
(`domain/`), porque la dependencia `domain` → `application` está prohibida. Tener tipo propio permite que el
caso de uso decida cómo tratar un dato inválido de la cátedra.

## Consecuencias
- **Desvío deliberado del skill** en la regla 1 de `casos-de-uso.md` §7. Se mantienen las reglas 2, 3 y 4
  (unchecked, sin HTTP, mensaje en inglés).
- Hay que agregar la carpeta `domain/exception/` a la estructura de paquetes de
  `arquitecturaHexagonalServicioCatalogo.md`. La regla `MODULO_SOLO_TIENE_TRES_CAPAS` no se ve afectada: la
  carpeta está dentro de `domain/`.
- Se asume que la cátedra publica datos válidos (ADR-0014): el caso de uso no la captura ni la trata de forma
  especial. Si llegara a lanzarse, la transacción de la sincronización se revierte y la versión local no avanza.
- La traducción a código HTTP, si algún día corresponde, la hace el controller (`@RestControllerAdvice`), no
  esta clase.
- Los tests unitarios del modelo (issue #3) verifican que se lance esta excepción por cada regla.
