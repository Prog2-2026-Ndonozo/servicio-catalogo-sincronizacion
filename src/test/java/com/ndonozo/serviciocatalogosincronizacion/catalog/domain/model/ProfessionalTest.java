package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfessionalTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    private static Professional.ProfessionalBuilder profesionalValido() {
        return Professional.builder()
                .id(15L)
                .categoryId(1L)
                .firstName("Ana")
                .lastName("Gomez")
                .enabled(true)
                .createdAt(CREADO)
                .updatedAt(ACTUALIZADO);
    }

    @Test
    void con_datos_validos_se_crea_con_los_valores_recibidos() {
        Professional profesional = profesionalValido().build();

        assertThat(profesional.getId()).isEqualTo(15L);
        assertThat(profesional.getCategoryId()).isEqualTo(1L);
        assertThat(profesional.getFirstName()).isEqualTo("Ana");
        assertThat(profesional.getLastName()).isEqualTo("Gomez");
        assertThat(profesional.isEnabled()).isTrue();
        assertThat(profesional.getCreatedAt()).isEqualTo(CREADO);
        assertThat(profesional.getUpdatedAt()).isEqualTo(ACTUALIZADO);
    }

    @Test
    void un_profesional_deshabilitado_es_valido() {
        Professional profesional = profesionalValido().enabled(false).build();

        assertThat(profesional.isEnabled()).isFalse();
    }

    @Test
    void sin_id_se_rechaza() {
        assertThatThrownBy(() -> profesionalValido().id(null).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("id");
    }

    @Test
    void sin_categoria_se_rechaza() {
        assertThatThrownBy(() -> profesionalValido().categoryId(null).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("categoryId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void el_nombre_nulo_vacio_o_en_blanco_se_rechaza(String nombreInvalido) {
        assertThatThrownBy(() -> profesionalValido().firstName(nombreInvalido).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("firstName");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void el_apellido_nulo_vacio_o_en_blanco_se_rechaza(String apellidoInvalido) {
        assertThatThrownBy(() -> profesionalValido().lastName(apellidoInvalido).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("lastName");
    }

    @Test
    void sin_fecha_de_creacion_se_rechaza() {
        assertThatThrownBy(() -> profesionalValido().createdAt(null).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("createdAt");
    }

    @Test
    void sin_fecha_de_actualizacion_se_rechaza() {
        assertThatThrownBy(() -> profesionalValido().updatedAt(null).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("updatedAt");
    }
}
