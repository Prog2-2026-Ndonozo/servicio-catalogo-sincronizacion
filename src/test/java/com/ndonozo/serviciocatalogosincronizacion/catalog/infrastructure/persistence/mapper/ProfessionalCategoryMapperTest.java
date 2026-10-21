package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalCategoryMapperTest {

    private static final Instant CREADA = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADA = Instant.parse("2026-09-10T13:40:00Z");

    private final ProfessionalCategoryMapper mapper = new ProfessionalCategoryMapper();

    private static ProfessionalCategory categoria() {
        return ProfessionalCategory.builder()
                .id(1L)
                .name("Clinica medica")
                .description("Atencion clinica general")
                .enabled(true)
                .createdAt(CREADA)
                .updatedAt(ACTUALIZADA)
                .build();
    }

    @Test
    void toEntity_copia_cada_campo_a_su_columna() {
        ProfessionalCategoryEntity entity = mapper.toEntity(categoria());

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo("Clinica medica");
        assertThat(entity.getDescription()).isEqualTo("Atencion clinica general");
        assertThat(entity.isEnabled()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(CREADA);
        assertThat(entity.getUpdatedAt()).isEqualTo(ACTUALIZADA);
    }

    @Test
    void ida_y_vuelta_conserva_todos_los_campos() {
        ProfessionalCategory original = categoria();

        ProfessionalCategory resultado = mapper.toDomain(mapper.toEntity(original));

        assertThat(resultado).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void null_devuelve_null_en_ambos_sentidos() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
