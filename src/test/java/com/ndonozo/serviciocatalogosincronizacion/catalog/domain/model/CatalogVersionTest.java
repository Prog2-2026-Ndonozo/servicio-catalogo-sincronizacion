package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CatalogVersionTest {

    private static CatalogVersion.CatalogVersionBuilder versionValida() {
        return CatalogVersion.builder()
                .snapshotVersion(1);
    }

    @Test
    void snapshot_version_se_crea_correctamente() {
        CatalogVersion version = versionValida().build();

        assertThat(version.getSnapshotVersion()).isEqualTo(1L);
    }

    @Test
    void snapshot_version_version_0_recibe_correctamente() {
        CatalogVersion version = versionValida().snapshotVersion(0).build();

        assertThat(version.getSnapshotVersion()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100, Long.MIN_VALUE})
    void snapshot_version_rechaza_version_negativa(long versionNegativa) {
        assertThatThrownBy(() -> versionValida().snapshotVersion(versionNegativa).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("snapshotVersion");
    }
}
