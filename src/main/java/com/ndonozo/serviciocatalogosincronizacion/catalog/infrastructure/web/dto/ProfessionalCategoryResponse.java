package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfessionalCategoryResponse(
        @Schema(description = "Id de la categoria en el catalogo de la catedra", example = "201")
        Long id,
        @Schema(description = " nombre ", example = "Cardiologia")
        String name,
        @Schema(description = "Descripcion de la categoria", example = "Atencion cardiovascular para el escenario academico")
        String description,
        @Schema(description = "false si la categoria está dado de baja", example = "true")
        boolean enabled
) {
}
