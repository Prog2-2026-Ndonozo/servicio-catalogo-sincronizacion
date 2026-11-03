package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionWindowTest {

    private static VersionWindow.VersionWindowBuilder ventanaValida() {
        return VersionWindow.builder()
                .currentVersion(7L)
                .oldestAvailableVersion(4L);
    }

    @Test
    void se_crea_correctamente_con_los_valores_del_contrato() {
        VersionWindow ventana = ventanaValida().build();

        assertThat(ventana.getCurrentVersion()).isEqualTo(7L);
        assertThat(ventana.getOldestAvailableVersion()).isEqualTo(4L);
    }

    @Test
    void acepta_ventana_de_una_sola_version() {
        VersionWindow ventana = ventanaValida().currentVersion(4L).build();

        assertThat(ventana.getCurrentVersion()).isEqualTo(4L);
        assertThat(ventana.getOldestAvailableVersion()).isEqualTo(4L);
    }

    @Test
    void acepta_catalogo_sin_versiones_publicadas() {
        VersionWindow ventana = ventanaValida().currentVersion(0L).oldestAvailableVersion(0L).build();

        assertThat(ventana.getCurrentVersion()).isZero();
        assertThat(ventana.getOldestAvailableVersion()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100})
    void rechaza_oldest_negativa(long oldestNegativa) {
        assertThatThrownBy(() -> ventanaValida().oldestAvailableVersion(oldestNegativa).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("OldestAvailableVersion");
    }

    @ParameterizedTest
    @CsvSource({"3, 4", "0, 1", "7, 8"})
    void rechaza_oldest_mayor_que_current(long current, long oldest) {
        assertThatThrownBy(() -> ventanaValida().currentVersion(current).oldestAvailableVersion(oldest).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("be greater than currentVersion");
    }

    @Test
    void rechaza_current_negativa_aunque_oldest_tambien_lo_sea() {
        assertThatThrownBy(() -> ventanaValida().currentVersion(-1L).oldestAvailableVersion(-2L).build())
                .isInstanceOf(InvalidCatalogDataException.class);
    }
}
