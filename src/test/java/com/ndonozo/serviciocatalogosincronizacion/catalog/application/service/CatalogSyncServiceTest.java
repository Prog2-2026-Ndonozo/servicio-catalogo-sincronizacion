package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.FullSyncRequiredException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncStatus;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyIncrementalChangeUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogVersionPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.SyncStatePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock
    private ApplyFullSnapshotUseCase applyFullSnapshot;

    @Mock
    private ApplyIncrementalChangeUseCase applyIncrementalChange;

    @Mock
    private CatalogVersionPort catalogVersion;

    @Mock
    private SyncStatePort syncState;

    @InjectMocks
    private CatalogSyncService service;

    private static CatalogVersion version(long value) {
        return CatalogVersion.builder().snapshotVersion(value).build();
    }

    private static CatalogUpdate update(long newVersion) {
        return CatalogUpdate.builder().eventId("catalog-event-" + newVersion).newVersion(newVersion).build();
    }

    // ---------- applyCatalogUpdate: evento aplicado o ignorado ----------

    @Test
    void un_evento_aplicado_se_delega_al_caso_de_uso_y_registra_ok() {
        CatalogUpdate evento = update(2);
        CatalogVersion aplicada = version(2);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(1));
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenReturn(aplicada);

        CatalogVersion resultado = service.applyCatalogUpdate(evento);

        assertThat(resultado).isSameAs(aplicada);
        verify(syncState).record(SyncStatus.OK, null);
        verifyNoMoreInteractions(syncState);
        verifyNoInteractions(applyFullSnapshot);
    }

    @Test
    void un_evento_ignorado_porque_la_version_no_cambio_no_escribe_el_estado() {
        CatalogUpdate evento = update(5);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(5));
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenReturn(version(5));

        CatalogVersion resultado = service.applyCatalogUpdate(evento);

        assertThat(resultado.getSnapshotVersion()).isEqualTo(5);
        verifyNoInteractions(syncState, applyFullSnapshot);
    }

    // ---------- applyCatalogUpdate: discontinuidad ----------

    @Test
    void si_el_incremental_exige_el_snapshot_lo_pide_y_registra_reconstruido_con_el_motivo() {
        CatalogUpdate evento = update(7);
        CatalogVersion delSnapshot = version(7);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(3));
        when(applyIncrementalChange.applyIncrementalChange(evento))
                .thenThrow(new FullSyncRequiredException("Version local 3 fuera de la ventana"));
        when(applyFullSnapshot.applyFullSnapshot()).thenReturn(delSnapshot);

        CatalogVersion resultado = service.applyCatalogUpdate(evento);

        assertThat(resultado).isSameAs(delSnapshot);
        verify(syncState).record(SyncStatus.RECONSTRUIDO_POR_DISCONTINUIDAD, "Version local 3 fuera de la ventana");
        verifyNoMoreInteractions(syncState);
    }

    @Test
    void si_ademas_falla_el_snapshot_de_respaldo_registra_error_y_relanza_la_misma_excepcion() {
        CatalogUpdate evento = update(7);
        CatedraUnavailableException error = new CatedraUnavailableException("Catedra answered HTTP 503", 503);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(3));
        when(applyIncrementalChange.applyIncrementalChange(evento))
                .thenThrow(new FullSyncRequiredException("fuera de la ventana"));
        when(applyFullSnapshot.applyFullSnapshot()).thenThrow(error);

        assertThatThrownBy(() -> service.applyCatalogUpdate(evento)).isSameAs(error);

        verify(syncState).record(SyncStatus.ERROR, "CatedraUnavailableException: Catedra answered HTTP 503");
        verifyNoMoreInteractions(syncState);
    }

    // ---------- applyCatalogUpdate: fallos ----------

    @Test
    void si_el_incremental_falla_registra_error_relanza_y_no_pide_el_snapshot() {
        CatalogUpdate evento = update(2);
        CatedraUnavailableException error = new CatedraUnavailableException("Redis de la catedra no disponible", 503);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(1));
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenThrow(error);

        assertThatThrownBy(() -> service.applyCatalogUpdate(evento)).isSameAs(error);

        verify(syncState).record(SyncStatus.ERROR, "CatedraUnavailableException: Redis de la catedra no disponible");
        verifyNoInteractions(applyFullSnapshot);
    }

    @Test
    void un_dato_invalido_tambien_registra_error_y_se_propaga_sin_ser_capturado() {
        CatalogUpdate evento = update(2);
        InvalidCatalogDataException error = new InvalidCatalogDataException("changes:2 ilegible");
        when(catalogVersion.getCatalogVersion()).thenReturn(version(1));
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenThrow(error);

        assertThatThrownBy(() -> service.applyCatalogUpdate(evento)).isSameAs(error);

        verify(syncState).record(SyncStatus.ERROR, "InvalidCatalogDataException: changes:2 ilegible");
        verifyNoInteractions(applyFullSnapshot);
    }

    @Test
    void si_no_se_puede_leer_la_version_local_no_se_llama_al_caso_de_uso_ni_se_escribe_el_estado() {
        when(catalogVersion.getCatalogVersion()).thenThrow(new IllegalStateException("base caida"));

        assertThatThrownBy(() -> service.applyCatalogUpdate(update(2))).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyIncrementalChange, applyFullSnapshot, syncState);
    }

    // ---------- el detalle del estado ----------

    @Test
    void el_detalle_del_error_se_recorta_a_500_caracteres() {
        CatalogUpdate evento = update(2);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(1));
        when(applyIncrementalChange.applyIncrementalChange(evento))
                .thenThrow(new IllegalStateException("x".repeat(2_000)));

        assertThatThrownBy(() -> service.applyCatalogUpdate(evento)).isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(syncState).record(any(SyncStatus.class), detalle.capture());
        assertThat(detalle.getValue()).hasSize(500).startsWith("IllegalStateException: xxx");
    }

    // ---------- un fallo al registrar el estado no cambia el resultado ----------

    @Test
    void si_falla_el_registro_de_ok_el_resultado_del_evento_aplicado_no_cambia() {
        CatalogUpdate evento = update(2);
        CatalogVersion aplicada = version(2);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(1));
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenReturn(aplicada);
        doThrow(new IllegalStateException("sync_state caida")).when(syncState).record(SyncStatus.OK, null);

        assertThat(service.applyCatalogUpdate(evento)).isSameAs(aplicada);
    }

    @Test
    void si_falla_el_registro_de_error_no_oculta_la_excepcion_original() {
        CatalogUpdate evento = update(2);
        CatedraUnavailableException original = new CatedraUnavailableException("Redis de la catedra no disponible", 503);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(1));
        when(applyIncrementalChange.applyIncrementalChange(evento)).thenThrow(original);
        doThrow(new IllegalStateException("sync_state caida")).when(syncState).record(any(SyncStatus.class), any());

        assertThatThrownBy(() -> service.applyCatalogUpdate(evento)).isSameAs(original);
    }

    // ---------- synchronizeIfEmpty ----------

    @Test
    void con_version_local_cero_sincroniza_registra_ok_y_devuelve_true() {
        when(catalogVersion.getCatalogVersion()).thenReturn(version(0));
        when(applyFullSnapshot.applyFullSnapshot()).thenReturn(version(7));

        assertThat(service.synchronizeIfEmpty()).isTrue();

        verify(applyFullSnapshot).applyFullSnapshot();
        verify(syncState).record(SyncStatus.OK, null);
        verifyNoInteractions(applyIncrementalChange);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 5, 55})
    void con_version_local_mayor_a_cero_no_sincroniza_ni_escribe_el_estado(long local) {
        when(catalogVersion.getCatalogVersion()).thenReturn(version(local));

        assertThat(service.synchronizeIfEmpty()).isFalse();

        verify(applyFullSnapshot, never()).applyFullSnapshot();
        verifyNoInteractions(syncState, applyIncrementalChange);
    }

    @Test
    void si_el_snapshot_de_arranque_falla_registra_error_y_relanza() {
        CatedraUnavailableException error = new CatedraUnavailableException("Catedra answered HTTP 503", 503);
        when(catalogVersion.getCatalogVersion()).thenReturn(version(0));
        when(applyFullSnapshot.applyFullSnapshot()).thenThrow(error);

        assertThatThrownBy(() -> service.synchronizeIfEmpty()).isSameAs(error);

        verify(syncState).record(SyncStatus.ERROR, "CatedraUnavailableException: Catedra answered HTTP 503");
        verifyNoMoreInteractions(syncState);
    }

    @Test
    void si_falla_el_registro_de_ok_el_arranque_igual_termina_bien() {
        when(catalogVersion.getCatalogVersion()).thenReturn(version(0));
        when(applyFullSnapshot.applyFullSnapshot()).thenReturn(version(7));
        doThrow(new IllegalStateException("sync_state caida")).when(syncState).record(SyncStatus.OK, null);

        assertThat(service.synchronizeIfEmpty()).isTrue();
    }

    @Test
    void si_no_se_puede_leer_la_version_local_al_arrancar_no_se_pide_el_snapshot() {
        when(catalogVersion.getCatalogVersion()).thenThrow(new IllegalStateException("base caída"));

        assertThatThrownBy(() -> service.synchronizeIfEmpty()).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(applyFullSnapshot, syncState);
    }
}
