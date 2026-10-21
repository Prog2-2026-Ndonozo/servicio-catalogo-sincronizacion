package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalMapperTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    private final ProfessionalMapper mapper = new ProfessionalMapper();

    private static Professional profesional() {
        return Professional.builder()
                .id(15L)
                .categoryId(1L)
                .firstName("Ana")
                .lastName("Gomez")
                .enabled(true)
                .createdAt(CREADO)
                .updatedAt(ACTUALIZADO)
                .build();
    }

    @Test
    void toEntity_copia_cada_campo_a_su_columna() {
        ProfessionalEntity entity = mapper.toEntity(profesional());

        assertThat(entity.getId()).isEqualTo(15L);
        assertThat(entity.getCategoryId()).isEqualTo(1L);
        assertThat(entity.getFirstName()).isEqualTo("Ana");
        assertThat(entity.getLastName()).isEqualTo("Gomez");
        assertThat(entity.isEnabled()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(CREADO);
        assertThat(entity.getUpdatedAt()).isEqualTo(ACTUALIZADO);
        // mapear profesional() con toEntity y comprobar cada campo de la entidad por separado
    }

    @Test
    void ida_y_vuelta_conserva_todos_los_campos() {
        Professional original = profesional();

        Professional resultado = mapper.toDomain(mapper.toEntity(original));

        assertThat(resultado).usingRecursiveComparison().isEqualTo(original);

        // toDomain(toEntity(original)) comparado con usingRecursiveComparison()
    }

    @Test
    void null_devuelve_null_en_ambos_sentidos() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
        // toEntity(null) y toDomain(null)
    }
}
