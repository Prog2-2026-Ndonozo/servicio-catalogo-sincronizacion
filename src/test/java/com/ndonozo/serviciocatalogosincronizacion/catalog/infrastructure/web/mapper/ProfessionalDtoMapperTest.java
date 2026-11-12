package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.exception.InvalidSearchRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfessionalDtoMapperTest {

    private final ProfessionalDtoMapper mapper = new ProfessionalDtoMapper();

    private static Professional profesional(boolean enabled) {
        return Professional.builder().id(201L).categoryId(101L).firstName("Ana").lastName("Torres").enabled(enabled)
                .createdAt(Instant.parse("2026-09-01T03:00:00Z")).updatedAt(Instant.parse("2026-11-06T14:19:29Z"))
                .build();
    }

    // ---------- toCriteria ----------

    @Test
    void sin_parametros_los_criterios_quedan_sin_filtros() {
        ProfessionalSearchCriteria criteria = mapper.toCriteria(null, null, null, null);

        assertThat(criteria.getCategoryId()).isNull();
        assertThat(criteria.getName()).isNull();
        assertThat(criteria.getEnabled()).isNull();
        assertThat(criteria.getAvailableOn()).isNull();
    }

    @Test
    void los_cuatro_parametros_pasan_a_los_criterios() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);

        ProfessionalSearchCriteria criteria = mapper.toCriteria(101L, "ana", true, fecha);

        assertThat(criteria.getCategoryId()).isEqualTo(101L);
        assertThat(criteria.getName()).isEqualTo("ana");
        assertThat(criteria.getEnabled()).isTrue();
        assertThat(criteria.getAvailableOn()).isEqualTo(fecha);
    }

    @Test
    void enabled_false_se_conserva() {
        assertThat(mapper.toCriteria(null, null, false, null).getEnabled()).isFalse();
    }

    @Test
    void un_nombre_en_blanco_equivale_a_no_filtrar() {
        assertThat(mapper.toCriteria(null, "   ", null, null).getName()).isNull();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void una_categoria_no_positiva_se_traduce_a_una_solicitud_invalida(long categoryId) {
        assertThatThrownBy(() -> mapper.toCriteria(categoryId, null, null, null))
                .isInstanceOf(InvalidSearchRequestException.class)
                .hasMessageContaining("categoryId");
    }

    @Test
    void un_nombre_demasiado_largo_se_traduce_a_una_solicitud_invalida() {
        String largo = "a".repeat(101);

        assertThatThrownBy(() -> mapper.toCriteria(null, largo, null, null))
                .isInstanceOf(InvalidSearchRequestException.class)
                .hasMessageContaining("name");
    }

    // ---------- toResponse ----------

    @Test
    void toResponse_copia_los_cinco_campos() {
        ProfessionalResponse response = mapper.toResponse(profesional(true));

        assertThat(response).isEqualTo(new ProfessionalResponse(201L, 101L, "Ana", "Torres", true));
    }

    @Test
    void toResponse_conserva_un_profesional_deshabilitado() {
        assertThat(mapper.toResponse(profesional(false)).enabled()).isFalse();
    }

    @Test
    void toResponse_con_null_devuelve_null() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
