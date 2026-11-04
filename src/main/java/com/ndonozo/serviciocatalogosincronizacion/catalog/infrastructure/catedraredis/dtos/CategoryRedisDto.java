package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryRedisDto(
        Long id,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

}
