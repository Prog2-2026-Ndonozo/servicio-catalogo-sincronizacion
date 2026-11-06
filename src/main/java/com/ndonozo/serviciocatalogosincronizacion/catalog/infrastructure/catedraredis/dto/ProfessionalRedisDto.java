package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfessionalRedisDto(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
