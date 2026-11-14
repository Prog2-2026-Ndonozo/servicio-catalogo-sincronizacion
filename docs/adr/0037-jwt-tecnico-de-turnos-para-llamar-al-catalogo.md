# ADR-0037: JWT técnico de turnos, firmado con RS256, para llamar al catálogo

Fecha: 2026-11-13
Estado: aceptada

## Contexto
E §9 exige "utilizar y validar JWT en la comunicación protegida entre los dos servicios" y deja a cargo del alumno
elegir entre propagar el JWT del usuario o usar un JWT técnico, justificando cómo se preservan la identidad, la
autorización y la trazabilidad, y validando "firma, vigencia y permisos" del token recibido.

Por el ADR-0036, el único cliente HTTP de este servicio es el servicio de turnos, y el registro y el login del usuario
final viven en turnos (ADR-0035). Este servicio nunca recibe tokens de usuario. Además, los datos que sirve
(profesionales, categorías, horarios) no pertenecen a ningún usuario: la propiedad por usuario (E §4.2) se aplica en
turnos.

El JWT técnico de la cátedra (I §2, I §4) es la credencial de la integración con el servicio central: lo usan los dos
backends hacia la cátedra y "nunca debe enviarse a KMP". Lo emite la cátedra, así que este servicio tampoco podría
validarlo.

## Opciones consideradas
1. **Propagar el JWT del usuario.**
   + Conserva la identidad hasta este servicio.
   − Este servicio tendría que validar tokens de usuario emitidos por turnos, y compartir material de firma con
     ellos.
   − Un token de usuario vencido rompe llamadas internas; los procesos en segundo plano (por ejemplo, recuperar
     reservas tras un reinicio) no tienen token de usuario.
2. **JWT técnico emitido por turnos, firmado con una clave compartida (HS256).**
   + Lo más simple de configurar.
   − Ambos servicios guardan el mismo secreto: este servicio podría emitir tokens que se hagan pasar por turnos.
   − Hace falta un decodificador propio (la configuración automática de Spring Boot no cubre claves simétricas).
3. **JWT técnico emitido por turnos, firmado con un par de claves (RS256).**
   + Solo turnos puede firmar; este servicio solo verifica, y su clave es pública (no es un secreto a custodiar).
   + Spring Boot arma el decodificador a partir de propiedades (clave pública y audiencia); solo hace falta un
     validador de una línea para el emisor.
   − Hay que generar el par de claves y entregar la pública a este servicio.
4. **Reutilizar el JWT técnico de la cátedra entre los dos servicios.**
   − Es una credencial de un tercero: se difundiría a un destino nuevo y este servicio no puede validar su firma.
5. **Un secreto estático o una clave de API.**
   − E §9 pide JWT con validación de firma y vigencia.

## Decisión
Opción 3. Turnos emite, para cada llamada a este servicio, un JWT **técnico** de vida corta firmado con RS256 (RSA de
2048 bits). Este servicio solo guarda la **clave pública** y valida cada token.

**Contenido del token:**

| Claim | Valor | Uso |
|---|---|---|
| `iss` | `servicio-turnos` | Se valida con un validador propio contra el emisor configurado (`CATALOGO_JWT_ISSUER`, por defecto `servicio-turnos`) |
| `aud` | `servicio-catalogo` | Se valida contra `audiences` |
| `sub` | `servicio-turnos` | Quién llama |
| `scope` | `catalog:read` | Permiso; Spring lo convierte en la autoridad `SCOPE_catalog:read` |
| `iat`, `exp` | `exp = iat + 5 minutos` | Vigencia corta |
| `jti` | identificador único | Trazabilidad en los registros |
| `userId` (opcional) | el usuario final en nombre de quien actúa turnos | **Solo informativo**: este servicio lo registra pero nunca autoriza por él |

**Validación en este servicio:** firma RS256, vigencia (`exp` y `nbf`, con la tolerancia de reloj por defecto de 60 s),
`iss`, `aud` y la autoridad `SCOPE_catalog:read` en todas las rutas de datos (búsqueda, categorías, agenda y estado).
Cualquier otra ruta se rechaza.

**Respuestas:** sin token o con token inválido, `401` con `WWW-Authenticate: Bearer`; con token válido sin el permiso,
`403`. Ambas con cuerpo `application/problem+json` y un `code` estable (`UNAUTHENTICATED`, `FORBIDDEN`), en la línea de
I §4 (las respuestas de Spring Security, por defecto, no traen cuerpo).

**Claves:** la privada solo existe en turnos y se carga desde una variable de entorno o un archivo fuera del repositorio;
la pública se entrega a este servicio con `spring.security.oauth2.resourceserver.jwt.public-key-location`, que apunta a
un archivo PEM. Ninguna clave se versiona.

**Sesión y CSRF:** el servicio es una API sin sesiones; las rutas de datos son de lectura y se autentican con el token
en cada pedido, por lo que se desactiva CSRF de forma deliberada.

**Documentación interactiva:** Swagger UI y `/v3/api-docs` solo son accesibles en el perfil de desarrollo, declarando el
esquema `bearer` en el documento para usar el botón "Authorize"; fuera de ese perfil quedan deshabilitados.

## Cómo justifica E §9
- **Identidad:** el llamador es siempre el servicio de turnos, verificado por su firma. El usuario final se identifica y
  autoriza en turnos; opcionalmente viaja en `userId` como dato firmado.
- **Autorización:** el permiso `catalog:read` limita lo que puede hacer el token. No hay rutas de escritura.
- **Trazabilidad:** `jti` y `userId` quedan en los registros de cada llamada (nunca el token completo).
- **Sin confianza en datos del cliente:** ningún identificador de usuario llega por parámetro o cabecera libre.

## Consecuencias
- **Turnos debe implementar:** generar el par de claves, firmar el token con los claims de la tabla, reutilizarlo hasta
  poco antes de que venza (5 minutos) y tratar `401` y `403` como errores de integración. Este contrato se documenta
  junto con los endpoints (E §12).
- **Sin revocación:** la vida corta del token (5 minutos) es la mitigación. Rotar la clave implica entregar la nueva
  pública a este servicio y reiniciarlo.
- **No se usa el JWT de la cátedra** entre los servicios.
- **No se configura `issuer-uri`:** con una clave pública local, Spring Boot 4.1.1 descarta la clave y busca las claves por
  red a esa dirección (falla con un texto que no es una URL). El emisor se valida con un `JwtIssuerValidator` propio. Lo
  comprobó una prueba con el decodificador real; una primera versión de este ADR suponía lo contrario.
- Un `userId` falso solo podría falsear un registro de trazabilidad, no una autorización, pero también proviene de un
  token firmado por turnos.
- **Pruebas:** token válido (`200`); sin token, con firma inválida, vencido, con otro emisor o con otra audiencia
  (`401`); sin el permiso (`403`). Las pruebas con el post-procesador `jwt()` de Spring Security no pasan por el
  decodificador, así que además se prueba el decodificador real con un par de claves generado en el test.
- **Pendiente al implementar:** cómo obtener un token de prueba para usar Swagger en desarrollo, y si el prefijo
  `/api/internal` se aplica a las rutas (ADR-0036).
