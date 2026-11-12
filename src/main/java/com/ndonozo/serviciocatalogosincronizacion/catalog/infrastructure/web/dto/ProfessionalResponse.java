package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfessionalResponse(
        @Schema(description = "ID del profesional en el catálogo de la cátedra", example = "201")
        Long id,
        @Schema(description = "ID de su categoría", example = "101")
        Long categoryId,
        @Schema(description = "Nombre", example = "Ana")
        String firstName,
        @Schema(description = "Apellido", example = "Torres")
        String lastName,
        @Schema(description = "false si el profesional está dado de baja", example = "true")
        boolean enabled) {
}
