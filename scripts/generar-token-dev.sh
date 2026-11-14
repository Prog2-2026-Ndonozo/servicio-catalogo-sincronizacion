#!/usr/bin/env bash
#
# Genera un JWT de PRUEBA firmado con RS256, igual al que emitiria el servicio de turnos (ADR-0037), para usarlo en
# desarrollo: pegarlo en el boton "Authorize" de Swagger UI o en la cabecera Authorization de curl.
#
# SOLO PARA DESARROLLO. Necesita la clave PRIVADA, que no debe estar dentro del repositorio ni subirse nunca.
#
# Uso:
#   scripts/generar-token-dev.sh /ruta/a/turnos-private.pem
#   CATALOGO_DEV_PRIVATE_KEY=/ruta/a/turnos-private.pem scripts/generar-token-dev.sh
#
# Variables opcionales (los valores por defecto coinciden con application.yaml):
#   TOKEN_MINUTOS     vigencia en minutos                 (5)
#   TOKEN_SCOPE       permiso del token                   (catalog:read)
#   TOKEN_EMISOR      claim iss                           (servicio-turnos)
#   TOKEN_AUDIENCIA   claim aud                           (servicio-catalogo)
#   TOKEN_USUARIO     claim userId, solo informativo      (sin usuario)
#
# El token sale por la salida estandar; los avisos, por la de errores. Ejemplo:
#   TOKEN=$(scripts/generar-token-dev.sh ~/claves-catalogo/turnos-private.pem)
#   curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/professional-categories

set -euo pipefail

CLAVE_PRIVADA="${1:-${CATALOGO_DEV_PRIVATE_KEY:-}}"
MINUTOS="${TOKEN_MINUTOS:-5}"
SCOPE="${TOKEN_SCOPE:-catalog:read}"
EMISOR="${TOKEN_EMISOR:-servicio-turnos}"
AUDIENCIA="${TOKEN_AUDIENCIA:-servicio-catalogo}"
USUARIO="${TOKEN_USUARIO:-}"

error() { echo "Error: $*" >&2; exit 1; }

command -v openssl >/dev/null || error "hace falta openssl"
[[ -n "$CLAVE_PRIVADA" ]] || error "indica la clave privada: $0 /ruta/a/turnos-private.pem"
[[ -r "$CLAVE_PRIVADA" ]] || error "no se puede leer la clave privada: $CLAVE_PRIVADA"
[[ "$MINUTOS" =~ ^[0-9]+$ && "$MINUTOS" -ge 1 ]] || error "TOKEN_MINUTOS debe ser un entero positivo"

# Estos valores se escriben en el JSON sin escapar: se aceptan solo caracteres seguros.
for valor in "$SCOPE" "$EMISOR" "$AUDIENCIA" "$USUARIO"; do
  [[ "$valor" =~ ^[A-Za-z0-9._:-]*$ ]] || error "caracteres no permitidos en '$valor' (solo letras, numeros y . _ : -)"
done

# La clave privada no deberia ser legible por otros usuarios.
permisos=$(stat -c '%a' "$CLAVE_PRIVADA" 2>/dev/null || echo "")
if [[ -n "$permisos" && "$permisos" != "600" && "$permisos" != "400" ]]; then
  echo "Aviso: la clave privada tiene permisos $permisos; conviene chmod 600 '$CLAVE_PRIVADA'" >&2
fi

base64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

jti=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)
ahora=$(date +%s)
vence=$((ahora + MINUTOS * 60))

cabecera='{"alg":"RS256","typ":"JWT"}'
usuario_json=""
[[ -n "$USUARIO" ]] && usuario_json=$(printf ',"userId":"%s"' "$USUARIO")
carga=$(printf '{"iss":"%s","aud":"%s","sub":"servicio-turnos","scope":"%s","iat":%d,"exp":%d,"jti":"%s"%s}' \
  "$EMISOR" "$AUDIENCIA" "$SCOPE" "$ahora" "$vence" "$jti" "$usuario_json")

parte_1=$(printf '%s' "$cabecera" | base64url)
parte_2=$(printf '%s' "$carga" | base64url)
firma=$(printf '%s.%s' "$parte_1" "$parte_2" | openssl dgst -sha256 -sign "$CLAVE_PRIVADA" | base64url)

echo "Token valido hasta $(date -d "@$vence" '+%Y-%m-%d %H:%M:%S') (${MINUTOS} min), permiso '$SCOPE'." >&2
printf '%s.%s.%s\n' "$parte_1" "$parte_2" "$firma"
