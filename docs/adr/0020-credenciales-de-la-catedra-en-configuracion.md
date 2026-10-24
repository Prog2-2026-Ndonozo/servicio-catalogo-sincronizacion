# ADR-0020: Credenciales de la cátedra por configuración externa y aislamiento del JWT

Fecha: 2026-10-24
Estado: aceptada

## Contexto
El servicio necesita la URL de la API de la cátedra y el JWT de su cuenta técnica (I §5). El JWT es un secreto:
no se versiona, no se loguea y no debe viajar por el código más de lo necesario (E §9). Las credenciales de la
base de datos ya se resuelven con un `.env` local que leen Docker Compose y la aplicación (issue #2).

## Opciones consideradas
1. **Valores fijos en `application.yaml`.**
   − Sube el secreto al repositorio.
2. **Leer un archivo `cuenta_tecnica.json` en tiempo de ejecución.**
   − Es un mecanismo aparte del resto de la configuración y exige código propio de lectura.
3. **Variables `CATEDRA_BASE_URL` y `CATEDRA_JWT` en el `.env`**, enlazadas con `@ConfigurationProperties`.
   + Mismo mecanismo que la base de datos. Una variable real del entorno tiene prioridad, lo que sirve para
     despliegues.
   + `@Validated` + `@NotBlank` hacen que la aplicación no arranque si falta alguno.

## Decisión
Opción 3.
- `CatedraProperties` (`config/`) es un `record` con prefijo `catedra` y valida que ambos valores no estén
  vacíos. Sobrescribe `toString` para mostrar `jwt=****`.
- `application.yaml` toma los valores de `${CATEDRA_BASE_URL}` y `${CATEDRA_JWT}`, sin valor por defecto.
- El `.env` real no se versiona; `.env.example` documenta las variables con valores de relleno.
- **El JWT solo existe en `config/`.** `RestClientConfig` lo coloca en el header `Authorization` al construir
  el `RestClient`. El cliente, el adaptador y el resto del código no lo ven.

## Justificación
Un solo mecanismo para toda la configuración sensible, con falla temprana si falta. Limitar el JWT a la
composition root reduce las rutas por las que podría filtrarse, y es coherente con la arquitectura: el
detalle de autenticación es un asunto de infraestructura que el núcleo desconoce.

## Consecuencias
- Sin `CATEDRA_BASE_URL` o `CATEDRA_JWT` la aplicación no arranca.
- `CatedraPropertiesTest` vigila que `toString` no exponga el token.
- Nunca se loguea el header `Authorization` ni el objeto de propiedades completo con el valor real.
- Reemplaza la mención de `cuenta_tecnica.json` en `README.md` y `AGENTS.md`; el archivo sigue en `.gitignore`
  por precaución.
