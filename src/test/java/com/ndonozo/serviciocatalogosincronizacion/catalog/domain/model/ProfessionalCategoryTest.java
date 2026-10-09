package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfessionalCategoryTest {

    private static final Instant CREADA = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADA = Instant.parse("2026-09-10T13:40:00Z");

    private static ProfessionalCategory.ProfessionalCategoryBuilder categoriaValida() {
        return ProfessionalCategory.builder()
                .id(1L)
                .name("Clinica medica")
                .description("Atencion clinica general")
                .enabled(true)
                .createdAt(CREADA)
                .updatedAt(ACTUALIZADA);
    }

    @Test
    void con_datos_validos_se_crea_con_los_valores_recibidos() {
        ProfessionalCategory categoria = categoriaValida().build();

        assertThat(categoria.getId()).isEqualTo(1L);
        assertThat(categoria.getName()).isEqualTo("Clinica medica");
        assertThat(categoria.getDescription()).isEqualTo("Atencion clinica general");
        assertThat(categoria.isEnabled()).isTrue();
        assertThat(categoria.getCreatedAt()).isEqualTo(CREADA);
        assertThat(categoria.getUpdatedAt()).isEqualTo(ACTUALIZADA);
    }

    @Test
    void una_categoria_deshabilitada_es_valida() {
        ProfessionalCategory categoria = categoriaValida().enabled(false).build();

        assertThat(categoria.isEnabled()).isFalse();
    }

    @Test
    void la_descripcion_vacia_es_valida() {
        ProfessionalCategory categoria = categoriaValida().description("").build();

        assertThat(categoria.getDescription()).isEmpty();
    }

    @Test
    void sin_descripcion_nula_se_rechaza() {
        assertThatThrownBy(() -> categoriaValida().description(null).build())
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void el_nombre_nulo_vacio_o_en_blanco_se_rechaza(String nombreInvalido) {
        assertThatThrownBy(() -> categoriaValida().name(nombreInvalido).build())
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void sin_id_se_rechaza() {
        assertThatThrownBy(() -> categoriaValida().id(null).build())
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void sin_fecha_de_creacion_se_rechaza() {
        assertThatThrownBy(() -> categoriaValida().createdAt(null).build())
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void sin_fecha_de_actualizacion_se_rechaza() {
        assertThatThrownBy(() -> categoriaValida().updatedAt(null).build())
                .isInstanceOf(InvalidCatalogDataException.class);
    }
}
