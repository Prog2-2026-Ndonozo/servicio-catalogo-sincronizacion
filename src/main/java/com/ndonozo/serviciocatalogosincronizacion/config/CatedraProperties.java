package com.ndonozo.serviciocatalogosincronizacion.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraProperties(
        @NotBlank String baseUrl,
        @NotBlank String jwt) {

    @Override
    public String toString() {
        return "CatedraProperties[baseUrl=" + baseUrl + ", jwt=****]";
    }
}