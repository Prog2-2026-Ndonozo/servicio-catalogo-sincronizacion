package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto;

public record ProfessionalCategoryResponse(
        Long id,
        String name,
        String description,
        boolean enabled
) {
}
