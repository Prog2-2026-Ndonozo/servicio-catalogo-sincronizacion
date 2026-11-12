package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalCategoryResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalCategoryDtoMapperTest {

    private final ProfessionalCategoryDtoMapper mapper = new ProfessionalCategoryDtoMapper();

    private static ProfessionalCategory categoria(boolean enabled, String descripcion) {
        return ProfessionalCategory.builder().id(101L).name("Clinica medica").description(descripcion).enabled(enabled)
                .createdAt(Instant.parse("2026-07-01T12:00:00Z")).updatedAt(Instant.parse("2026-09-10T13:40:00Z")).build();
    }

    @Test
    void copia_los_cuatro_campos() {
        assertThat(mapper.toResponse(categoria(true, "Atencion clinica general")))
                .isEqualTo(new ProfessionalCategoryResponse(101L, "Clinica medica", "Atencion clinica general", true));
    }

    @Test
    void conserva_una_categoria_deshabilitada() {
        assertThat(mapper.toResponse(categoria(false, "")).enabled()).isFalse();
    }

    @Test
    void con_null_devuelve_null() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
