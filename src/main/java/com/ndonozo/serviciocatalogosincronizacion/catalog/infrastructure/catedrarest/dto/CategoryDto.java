package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto;

import java.time.Instant;

public record CategoryDto(
        Long id,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
