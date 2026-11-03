package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogUpdateTest {

    private static CatalogUpdate.CatalogUpdateBuilder updateValido() {
        return CatalogUpdate.builder()
                .eventId("catalog-event-7-4f3b")
                .newVersion(7);
    }

    @Test
    void se_crea_correctamente_con_datos_validos() {
        CatalogUpdate update = updateValido().build();

        assertThat(update.getEventId()).isEqualTo("catalog-event-7-4f3b");
        assertThat(update.getNewVersion()).isEqualTo(7L);
    }

    @Test
    void acepta_la_version_mas_chica_posible() {
        CatalogUpdate update = updateValido().newVersion(1).build();

        assertThat(update.getNewVersion()).isEqualTo(1L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void rechaza_event_id_nulo_vacio_o_en_blanco(String eventIdInvalido) {
        assertThatThrownBy(() -> updateValido().eventId(eventIdInvalido).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("eventId");
    }

    @Test
    void rechaza_version_cero_porque_es_el_estado_inicial_y_no_se_publica() {
        assertThatThrownBy(() -> updateValido().newVersion(0).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("newVersion");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100, Long.MIN_VALUE})
    void rechaza_version_negativa(long versionNegativa) {
        assertThatThrownBy(() -> updateValido().newVersion(versionNegativa).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("newVersion");
    }
}
