# Contrato entre servicio-catalogo y servicio-turnos (v1)

Contrato del servicio de catálogo y sincronización para su único cliente HTTP: el servicio de turnos y reservas.
Cumple E §12 (contratos entre los dos servicios: operaciones, DTO, autenticación y errores) y se apoya en
ADR-0034, ADR-0035, ADR-0036 y ADR-0037.

> **Alcance.** La interfaz KMP **no** consulta a este servicio (ADR-0036, I §17). Si KMP necesita buscar profesionales,
> lo hace a través de turnos, que consulta a este servicio. Este servicio no recibe ni valida tokens de usuario final.

## 1. Convenciones

- JSON en `camelCase`; fechas `yyyy-MM-dd`; horas `HH:mm:ss` (hora local de atención); días de la semana `MONDAY` a
  `SUNDAY`. Las mismas convenciones que I §4.
- Todas las rutas de datos son de **solo lectura** (`GET`).
- Un cliente debe decidir por el **código HTTP** y por `code` (cuando existe), nunca por el texto de `detail`.
- Un cliente debe **ignorar los campos que no conozca**: las respuestas pueden ganar campos opcionales dentro de v1.
- Todos los datos salen de la copia local del catálogo; este servicio no consulta a la cátedra para responder.

## 2. Autenticación

Cada pedido lleva un **JWT técnico emitido por turnos** (ADR-0037):

```
Authorization: Bearer <jwt>
```

| Aspecto | Valor |
|---|---|
| Algoritmo | `RS256` (RSA de 2048 bits). Turnos firma con su clave **privada**; este servicio solo verifica con la **pública** |
| `iss` | `servicio-turnos` |
| `aud` | `servicio-catalogo` |
| `sub` | `servicio-turnos` |
| `scope` | `catalog:read` |
| `iat` y `exp` | `exp = iat + 5 minutos` (recomendado). La tolerancia de reloj es de 60 segundos |
| `jti` | Identificador único del token |
| `userId` (opcional) | Usuario final en nombre de quien actúa turnos. **Solo informativo**: el catálogo lo registra pero nunca autoriza por él |

Este servicio valida firma, vigencia (`exp`, `nbf`), emisor, audiencia y que `scope` contenga `catalog:read`. No hay
revocación: la vida corta del token es la mitigación.

**No se debe reutilizar el JWT técnico de la cátedra** (I §2): es otra credencial y este servicio no puede validarlo.

### Cómo emitirlo desde turnos

Hace falta la librería `com.nimbusds:nimbus-jose-jwt` (o `spring-security-oauth2-jose`, que la incluye):

```java
public class CatalogTokenIssuer {

    private static final Duration VIGENCIA = Duration.ofMinutes(5);
    private final RSAPrivateKey clavePrivada;

    public CatalogTokenIssuer(RSAPrivateKey clavePrivada) {
        this.clavePrivada = clavePrivada;
    }

    /** userId es opcional y solo informativo. */
    public String emitir(String userId) {
        Instant ahora = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("servicio-turnos")
                .audience("servicio-catalogo")
                .subject("servicio-turnos")
                .claim("scope", "catalog:read")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plus(VIGENCIA)));
        if (userId != null) {
            claims.claim("userId", userId);
        }
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
            jwt.sign(new RSASSASigner(clavePrivada));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token para el catalogo", e);
        }
    }
}
```

Imports: `com.nimbusds.jose.{JOSEException, JWSAlgorithm, JWSHeader}`, `com.nimbusds.jose.crypto.RSASSASigner`,
`com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}`, `java.security.interfaces.RSAPrivateKey`, `java.time.{Duration, Instant}`,
`java.util.{Date, UUID}`.

**Uso recomendado:** emitir un token y reutilizarlo hasta unos 30 segundos antes de que venza. Si el catálogo responde
`401`, emitir uno nuevo y reintentar **una sola vez**.

### Claves

Se generan una vez; la privada solo existe en turnos y **nunca se versiona**:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out turnos-private.pem
openssl rsa -pubout -in turnos-private.pem -out turnos-public.pem
```

La clave pública se entrega al catálogo con la variable `CATALOGO_JWT_PUBLIC_KEY_LOCATION` (por ejemplo
`file:/ruta/turnos-public.pem`). Si hay que rotar las claves, se entrega la nueva pública y se reinicia el catálogo.

### Configuración del catálogo

| Variable | Obligatoria | Por defecto | Uso |
|---|---|---|---|
| `CATALOGO_JWT_PUBLIC_KEY_LOCATION` | Sí | — | Archivo PEM con la clave pública de turnos. Sin ella el servicio **no arranca** |
| `CATALOGO_JWT_ISSUER` | No | `servicio-turnos` | Emisor esperado (`iss`) |
| `CATALOGO_JWT_AUDIENCE` | No | `servicio-catalogo` | Audiencia esperada (`aud`) |

## 3. Operaciones

| Operación | Ruta | Para qué |
|---|---|---|
| Buscar profesionales | `GET /api/professionals/search` | Búsqueda con filtros opcionales |
| Listar categorías | `GET /api/professional-categories` | Opciones para el filtro por categoría |
| Agenda de un profesional | `GET /api/professionals/{id}/agenda` | El profesional y todos sus horarios semanales |
| Estado de sincronización | `GET /api/sync/status` | **Pendiente** (issue #12): todavía no existe |

### 3.1. `GET /api/professionals/search`

Todos los parámetros son opcionales y, si hay varios, se combinan con **AND**. Sin parámetros devuelve todos los
profesionales. No hay paginación. El orden es apellido, nombre e id.

| Parámetro | Tipo | Descripción |
|---|---|---|
| `categoryId` | entero positivo | Categoría del profesional |
| `name` | texto, máx. 100 | Aparece dentro del nombre o del apellido, o de ambos juntos (`ana torres`, `torres`). No distingue mayúsculas; **sí distingue tildes**. Un texto en blanco equivale a no filtrar |
| `enabled` | `true` o `false` | `true`: solo habilitados; `false`: solo deshabilitados |
| `availableOn` | fecha `yyyy-MM-dd` | Profesionales que **atienden ese día de la semana**: con al menos un horario habilitado para él |

**`availableOn` es un prefiltro, no una consulta de turnos libres** (ADR-0034). Las ocupaciones las resuelve turnos con
la cátedra (E §7, I §8). Los filtros son independientes: `availableOn` no implica que el profesional esté habilitado.

```
GET /api/professionals/search?categoryId=101&enabled=true&availableOn=2026-11-23
```

`200`:

```json
[
  { "id": 15, "categoryId": 101, "firstName": "Ana", "lastName": "Gomez", "enabled": true }
]
```

Sin coincidencias responde `200` con `[]`. Errores: `400` con `VALIDATION_ERROR` (categoría no positiva o nombre de más de
100 caracteres) o `400` por un valor mal formado (fecha o booleano ilegibles, categoría no numérica).

### 3.2. `GET /api/professional-categories`

Sin parámetros. Devuelve **todas** las categorías, habilitadas o no, ordenadas por nombre e id.

`200`:

```json
[
  { "id": 101, "name": "Cardiologia", "description": "Atencion cardiovascular", "enabled": true }
]
```

### 3.3. `GET /api/professionals/{id}/agenda`

Devuelve el profesional y **todos** sus horarios semanales, habilitados o no, ordenados por día de la semana (lunes
primero), hora de inicio e id.

`200`:

```json
{
  "professional": { "id": 201, "categoryId": 101, "firstName": "Ana", "lastName": "Torres", "enabled": true },
  "weeklySchedules": [
    { "id": 301, "dayOfWeek": "MONDAY", "startTime": "12:00:00", "endTime": "15:00:00",
      "slotDurationMinutes": 30, "enabled": true },
    { "id": 302, "dayOfWeek": "WEDNESDAY", "startTime": "17:00:00", "endTime": "20:00:00",
      "slotDurationMinutes": 30, "enabled": false }
  ]
}
```

- Un profesional sin horarios da `200` con `weeklySchedules: []`.
- Un profesional **deshabilitado** también tiene agenda, con `enabled: false`.
- Errores: `404` con `PROFESSIONAL_NOT_FOUND` si el profesional no existe (un ID no positivo se trata igual); `400`
  por un ID no numérico.

## 4. Modelos

**`ProfessionalResponse`**

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | entero | ID del profesional en el catálogo de la cátedra |
| `categoryId` | entero | ID de su categoría |
| `firstName` | texto | Nombre |
| `lastName` | texto | Apellido |
| `enabled` | booleano | `false` si está dado de baja |

**`ProfessionalCategoryResponse`**

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | entero | ID de la categoría |
| `name` | texto | Nombre |
| `description` | texto | Descripción; puede ser vacía |
| `enabled` | booleano | `false` si está dada de baja |

**`WeeklyScheduleResponse`**

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | entero | ID del horario |
| `dayOfWeek` | texto | `MONDAY` a `SUNDAY` |
| `startTime` | hora | Inicio de la atención (`HH:mm:ss`) |
| `endTime` | hora | Fin de la atención (`HH:mm:ss`) |
| `slotDurationMinutes` | entero | Duración de cada turno, en minutos |
| `enabled` | booleano | `false` si está dado de baja |

**`ProfessionalAgendaResponse`**: `professional` (`ProfessionalResponse`) y `weeklySchedules` (lista de
`WeeklyScheduleResponse`).

Las fechas de creación y actualización de la cátedra **no** se exponen.

## 5. Errores

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Parámetro de búsqueda inválido (categoría no positiva, nombre demasiado largo) |
| 400 | sin `code` | Valor mal formado que detecta Spring (fecha, booleano o número ilegibles; ID no numérico) |
| 401 | `UNAUTHENTICATED` | Token ausente, mal formado, con firma inválida, vencido, de otro emisor o de otra audiencia. Incluye `WWW-Authenticate: Bearer` |
| 403 | `FORBIDDEN` | Token válido sin el permiso `catalog:read`, o ruta que no es de datos |
| 404 | `PROFESSIONAL_NOT_FOUND` | El profesional de la agenda no existe |
| 404 | sin `code` | Ruta inexistente |
| 405 | sin `code` | Método distinto de `GET` |
| 500, 503 | sin garantía | Fallo interno o de la base de datos |

Los errores con `code` son `application/problem+json`:

```json
{ "type": "about:blank", "title": "Not Found", "status": 404,
  "detail": "No existe un profesional con id 201", "code": "PROFESSIONAL_NOT_FOUND" }
```

Los que figuran "sin `code`" usan el formato de error por defecto de Spring (`timestamp`, `status`, `error`, `path`).
El `detail` de `401` y `403` es genérico a propósito: no revela por qué se rechazó el token.

## 6. Reglas de uso para turnos

1. **Ignorar los horarios con `enabled: false`** al ofrecer turnos. La cátedra rechaza con `INVALID_SLOT` un turno fuera
   de la agenda habilitada (I §13.1).
2. **No ofrecer turnos de un profesional con `enabled: false`**: la cátedra responde `PROFESSIONAL_DISABLED` (I §13.1).
3. **No conservar el catálogo como "vigente".** Turnos no puede mantener una réplica para búsquedas o nuevas reservas
   (E §3, E §4.2): debe consultar a este servicio antes de cada operación que lo necesite. Puede guardar datos
   descriptivos históricos en una reserva.
4. **Si el catálogo no responde, no iniciar operaciones que requieran validar información vigente** (E §8).
5. Todos los pedidos son `GET` idempotentes: se pueden reintentar con seguridad. Los reintentos deben ser limitados y
   observables (I §18.4). El contrato no fija timeouts; se recomienda uno corto (unos segundos) en turnos.
6. Los datos del catálogo pueden quedar atrasados unos instantes respecto de la cátedra mientras se sincroniza; este
   servicio converge solo (ADR-0028, ADR-0029, ADR-0031).

## 7. Cómo probarlo

Con el perfil de desarrollo, la documentación interactiva queda en `http://localhost:8080/swagger-ui.html` (ADR-0037):

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
TOKEN=$(scripts/generar-token-dev.sh /ruta/a/turnos-private.pem)
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/professionals/search?name=ana"
```

`scripts/generar-token-dev.sh` solo sirve para desarrollo: firma un token igual al que emitiría turnos y necesita la
clave privada.

## 8. Versión

Este documento describe la **v1**. Los cambios compatibles (campos nuevos opcionales, rutas nuevas) no cambian la
versión; los incompatibles requerirían un ADR y avisar a turnos.
