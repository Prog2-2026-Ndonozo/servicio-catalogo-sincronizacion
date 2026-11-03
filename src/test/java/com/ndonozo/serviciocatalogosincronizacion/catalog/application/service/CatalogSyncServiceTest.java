package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyIncrementalChangeUseCase;
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
    private ApplyIncrementalChangeUseCase applyIncrementalChange;

    @Mock
    private CatalogRepositoryPort repository;

    @InjectMocks
    private CatalogSyncService service;

    private static CatalogVersion version(long value) {
        return CatalogVersion.builder().snapshotVersion(value).build();
    }

    private static CatalogUpdate update(long newVersion) {
        return CatalogUpdate.builder().eventId("catalog-event-" + newVersion).newVersion(newVersion).build();
    }

    @Test
    void aplicar_un_catalog_update_lo_delega_al_caso_de_uso_incremental_y_devuelve_su_version() {
        CatalogUpdate evento = update(7);
        CatalogVersion aplicada = version(7);
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenReturn(aplicada);

        CatalogVersion resultado = service.applyCatalogUpdate(evento);

        assertThat(resultado).isSameAs(aplicada);
        verify(applyIncrementalChange).applyIncrementalChange(evento);
    }

    @Test
    void si_el_caso_de_uso_incremental_falla_la_excepcion_se_propaga_sin_ser_capturada() {
        CatalogUpdate evento = update(7);
        InvalidCatalogDataException error = new InvalidCatalogDataException("cambio invalido");
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenThrow(error);

        assertThatThrownBy(() -> service.applyCatalogUpdate(evento)).isSameAs(error);
    }

    @Test
    void aplicar_un_catalog_update_no_toca_el_snapshot_completo_ni_el_repositorio() {
        CatalogUpdate evento = update(7);
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenReturn(version(7));

        service.applyCatalogUpdate(evento);

        verifyNoInteractions(applyFullSnapshot, repository);
    }

    @Test
    void sincronizar_si_esta_vacio_no_usa_el_caso_de_uso_incremental() {
        when(repository.getCatalogVersion()).thenReturn(version(0));

        service.synchronizeIfEmpty();

        verifyNoInteractions(applyIncrementalChange);
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
