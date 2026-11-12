package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfessionalSearchCriteriaTest {

    @Test
    void sin_ningun_filtro_todos_los_campos_quedan_en_null() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().build();

        assertThat(criteria.getCategoryId()).isNull();
        assertThat(criteria.getName()).isNull();
        assertThat(criteria.getEnabled()).isNull();
        assertThat(criteria.getAvailableOn()).isNull();
    }

    @Test
    void conserva_los_cuatro_filtros_presentes() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);

        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder()
                .categoryId(3L).name("ana").enabled(true).availableOn(fecha).build();

        assertThat(criteria.getCategoryId()).isEqualTo(3L);
        assertThat(criteria.getName()).isEqualTo("ana");
        assertThat(criteria.getEnabled()).isTrue();
        assertThat(criteria.getAvailableOn()).isEqualTo(fecha);
    }

    @Test
    void enabled_false_se_conserva_y_no_equivale_a_sin_filtro() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().enabled(false).build();

        assertThat(criteria.getEnabled()).isFalse();
    }

    // ---------- nombre ----------

    @Test
    void el_nombre_se_recorta_de_espacios() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().name("   Ana Torres \t").build();

        assertThat(criteria.getName()).isEqualTo("Ana Torres");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void un_nombre_ausente_o_en_blanco_equivale_a_no_filtrar(String name) {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().name(name).build();

        assertThat(criteria.getName()).isNull();
    }

    @Test
    void un_nombre_de_exactamente_100_caracteres_es_valido() {
        String cien = "a".repeat(100);

        assertThat(ProfessionalSearchCriteria.builder().name(cien).build().getName()).isEqualTo(cien);
    }

    @Test
    void los_espacios_sobrantes_no_cuentan_para_el_limite_de_100() {
        String cien = "a".repeat(100);

        assertThat(ProfessionalSearchCriteria.builder().name("  " + cien + "  ").build().getName()).isEqualTo(cien);
    }

    @Test
    void un_nombre_de_mas_de_100_caracteres_se_rechaza() {
        String largo = "a".repeat(101);

        assertThatThrownBy(() -> ProfessionalSearchCriteria.builder().name(largo).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("name")
                .hasMessageContaining("100");
    }

    @Test
    void el_nombre_conserva_los_caracteres_comodin_tal_cual_porque_el_escape_es_del_adaptador() {
        assertThat(ProfessionalSearchCriteria.builder().name("50%_a").build().getName()).isEqualTo("50%_a");
    }

    // ---------- categoria ----------

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    void una_categoria_no_positiva_se_rechaza(long categoryId) {
        assertThatThrownBy(() -> ProfessionalSearchCriteria.builder().categoryId(categoryId).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("categoryId")
                .hasMessageContaining(String.valueOf(categoryId));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 101, Long.MAX_VALUE})
    void una_categoria_positiva_es_valida(long categoryId) {
        assertThat(ProfessionalSearchCriteria.builder().categoryId(categoryId).build().getCategoryId())
                .isEqualTo(categoryId);
    }
}
