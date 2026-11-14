package com.ndonozo.serviciocatalogosincronizacion.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Pares de claves RSA generados al vuelo y un constructor de tokens para probar la seguridad (ADR-0037). El servicio
 * de turnos se simula con CLAVES_DE_TURNOS; CLAVES_AJENAS sirve para firmar tokens que no deben aceptarse. La clave
 * publica de turnos se entrega al servidor de recursos con las mismas propiedades que se usan en produccion.
 */
public abstract class JwtTestSupport {

    protected static final String EMISOR = "servicio-turnos";
    protected static final String AUDIENCIA = "servicio-catalogo";
    protected static final String PERMISO = "catalog:read";

    private static final KeyPair CLAVES_DE_TURNOS = generarClaves();
    protected static final KeyPair CLAVES_AJENAS = generarClaves();
    private static final Path CLAVE_PUBLICA_DE_TURNOS = escribirClavePublica(CLAVES_DE_TURNOS);

    @DynamicPropertySource
    static void propiedadesDeSeguridad(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.public-key-location",
                () -> "file:" + CLAVE_PUBLICA_DE_TURNOS);
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> AUDIENCIA);
    }

    protected static TokenBuilder token() {
        return new TokenBuilder();
    }

    /** Cabecera Authorization con un token valido de turnos. */
    protected static String tokenValido() {
        return "Bearer " + token().build();
    }

    protected static final class TokenBuilder {
        private KeyPair firmante = CLAVES_DE_TURNOS;
        private String emisor = EMISOR;
        private String audiencia = AUDIENCIA;
        private String permiso = PERMISO;
        private Instant emitido = Instant.now();
        private Instant vence = Instant.now().plusSeconds(300);
        private Instant noAntes;
        private String usuario;

        TokenBuilder() {
        }

        public TokenBuilder firmadoPor(KeyPair claves) {
            this.firmante = claves;
            return this;
        }

        public TokenBuilder emisor(String emisor) {
            this.emisor = emisor;
            return this;
        }

        public TokenBuilder audiencia(String audiencia) {
            this.audiencia = audiencia;
            return this;
        }

        public TokenBuilder permiso(String permiso) {
            this.permiso = permiso;
            return this;
        }

        public TokenBuilder vencidoHace(long segundos) {
            this.emitido = Instant.now().minusSeconds(segundos + 300);
            this.vence = Instant.now().minusSeconds(segundos);
            return this;
        }

        public TokenBuilder validoDesdeDentroDe(long segundos) {
            this.noAntes = Instant.now().plusSeconds(segundos);
            return this;
        }

        public TokenBuilder enNombreDe(String usuario) {
            this.usuario = usuario;
            return this;
        }

        public String build() {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(emisor)
                    .audience(audiencia)
                    .subject("servicio-turnos")
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(emitido))
                    .expirationTime(Date.from(vence));
            if (permiso != null) {
                claims.claim("scope", permiso);
            }
            if (noAntes != null) {
                claims.notBeforeTime(Date.from(noAntes));
            }
            if (usuario != null) {
                claims.claim("userId", usuario);
            }
            try {
                SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
                jwt.sign(new RSASSASigner(firmante.getPrivate()));
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new IllegalStateException("No se pudo firmar el token de prueba", e);
            }
        }
    }

    private static KeyPair generarClaves() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path escribirClavePublica(KeyPair claves) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(claves.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n";
        try {
            Path archivo = Files.createTempFile("turnos-public-", ".pem");
            Files.writeString(archivo, pem);
            archivo.toFile().deleteOnExit();
            return archivo;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
