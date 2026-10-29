package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock
    private ApplyFullSnapshotUseCase applyFullSnapshot;

    @Mock
    private CatalogRepositoryPort repository;

    @InjectMocks
    private CatalogSyncService service;

    private static CatalogVersion version(long value) {
        return CatalogVersion.builder().snapshotVersion(value).build();
    }

    @Test
    void con_version_local_cero_sincroniza_y_devuelve_true() {
        when(repository.getCatalogVersion()).thenReturn(version(0));

        boolean sincronizo = service.synchronizeIfEmpty();

        assertThat(sincronizo).isTrue();
        verify(applyFullSnapshot).applyFullSnapshot();
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 7, 1_000})
    void con_version_local_mayor_a_cero_no_sincroniza_y_devuelve_false(long local) {
        when(repository.getCatalogVersion()).thenReturn(version(local));

        boolean sincronizo = service.synchronizeIfEmpty();

        assertThat(sincronizo).isFalse();
        verify(applyFullSnapshot, never()).applyFullSnapshot();
    }

    @Test
    void si_el_caso_de_uso_falla_la_excepcion_se_propaga() {
        when(repository.getCatalogVersion()).thenReturn(version(0));
        CatedraUnavailableException error = new CatedraUnavailableException("cátedra caída", 503);
        when(applyFullSnapshot.applyFullSnapshot()).thenThrow(error);

        assertThatThrownBy(() -> service.synchronizeIfEmpty()).isSameAs(error);
    }

    @Test
    void si_no_se_puede_leer_la_version_local_no_se_pide_el_snapshot() {
        when(repository.getCatalogVersion()).thenThrow(new IllegalStateException("base caída"));

        assertThatThrownBy(() -> service.synchronizeIfEmpty())
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyFullSnapshot);
    }
}
