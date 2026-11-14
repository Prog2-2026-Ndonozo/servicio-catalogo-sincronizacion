package com.ndonozo.serviciocatalogosincronizacion.config;

import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.security.ProblemJsonSecurityHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Servidor de recursos (ADR-0037): solo se aceptan JWT firmados por el servicio de turnos, con el permiso
 * catalog:read. Sin sesiones; CSRF desactivado a proposito porque es una API de solo lectura con token por pedido.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    static final String PERMISO_DE_LECTURA = "SCOPE_catalog:read";

    private final ProblemJsonSecurityHandlers handlers;

    /**
     * Valida que el token lo haya emitido el servicio de turnos. Spring Boot no lo hace solo cuando la clave publica
     * es local: si se configura issuer-uri, descarta la clave y busca las claves por red (ADR-0037).
     */
    @Bean
    OAuth2TokenValidator<Jwt> emisorEsperado(@Value("${CATALOGO_JWT_ISSUER:servicio-turnos}") String emisor) {
        return new JwtIssuerValidator(emisor);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/**").hasAuthority(PERMISO_DE_LECTURA)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));
        return http.build();
    }

    /** Solo en desarrollo: la documentacion interactiva se abre sin token (en otros perfiles esta deshabilitada). */
    @Bean
    @Order(1)
    @Profile("dev")
    SecurityFilterChain docsSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}