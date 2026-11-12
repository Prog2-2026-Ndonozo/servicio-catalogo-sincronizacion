package com.ndonozo.serviciocatalogosincronizacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI catalogOpenApi(){
        return new OpenAPI().info(new Info()
                .title("Servicio de catalogo y sincronizacion")
                .version("v1")
                .description("Copia local del catalogo de la catedra (categorias, profesionales y horarios semanales). " +
                        " Las busquedas se resuelven solo con datos locales")

        );
    }
}
