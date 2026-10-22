# ADR-0015: Modelo de dominio inmutable que se valida al construirse (desvío del skill)

Fecha: 2026-10-06
Estado: aceptada

## Contexto
El skill `hexagonal-catalogo` (`casos-de-uso.md` §1) define el modelo de dominio como una clase Lombok mutable con
`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor` y `@AllArgsConstructor` (regla 2), y a la vez establece que
"los invariantes van acá": las validaciones se hacen en el modelo, que es la única capa donde no se pueden
esquivar (regla 3). Las dos reglas son incompatibles: los constructores y setters que genera Lombok no validan,
así que con el constructor vacío y un setter se crea un objeto inválido. Las validaciones del ADR-0014 (nombres no
vacíos, `startTime` anterior a `endTime`, rango múltiplo de la duración, versión no negativa) quedarían sin garantía.
El skill justifica el constructor vacío por Jackson y los mappers; en este dominio Jackson está prohibido
(`DOMINIO_ES_PURO`) y los mappers pueden crear objetos con el builder.

## Opciones consideradas
1. **Plantilla del skill tal cual** (clase mutable, cuatro constructores de Lombok).
   + Coincide con el skill y con el proyecto de referencia.
   − Las validaciones se pueden esquivar; la regla 3 del propio skill no se cumple.
2. **Clase inmutable con Lombok y constructor que valida.** Sin setters ni constructor vacío; se crea solo con
   datos válidos, mediante `@Builder`.
   + Un objeto inválido no puede existir; conserva Lombok y el builder.
   − Se aparta de la regla 2 del skill; los mappers deben usar el builder.
3. **`record` de Java.** Inmutable y con validación en su constructor.
   + Compacto, sin Lombok.
   − Constructor posicional con diez datos, varios del mismo tipo (`Long`, `Instant`): fácil confundir el orden.

## Decisión
Opción 2. Cada concepto del modelo (`ProfessionalCategory`, `Professional`, `WeeklySchedule`, `CatalogVersion`) es
una clase **inmutable**: sin setters ni constructor vacío, creada con un builder cuyo constructor valida las reglas
del ADR-0014 y falla si el dato es inválido.

## Justificación
Hace cumplir en el propio modelo las validaciones acordadas, que es lo que el skill pide en su regla 3. Se
sacrifica la regla 2 porque su motivo (Jackson) no aplica a nuestro dominio y porque ambas reglas no pueden
cumplirse a la vez.

## Consecuencias
- **Desvío deliberado del skill** en la regla 2 de `casos-de-uso.md` §1. Las demás reglas (cero framework, objeto
  plano, invariantes en el modelo) se mantienen.
- Los mappers de `infrastructure/` crean los objetos de dominio con el builder; no hay forma de crear uno vacío
  y completarlo después.
- Un dato inválido publicado por la cátedra provoca un error al construir el objeto. Qué hacer con ese error en
  la sincronización queda abierto (ADR-0014, issues #5 y #6).
- Los tests unitarios del modelo (issue #3) verifican que cada regla impide construir el objeto.
