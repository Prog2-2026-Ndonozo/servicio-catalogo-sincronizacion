package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto;

import java.time.Instant;

public record ProfessionalDto(
        Long id,
        Long categoryId,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
