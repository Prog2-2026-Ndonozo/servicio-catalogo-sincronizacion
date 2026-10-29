package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.startup;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.CatalogSyncService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartupSyncRunnerTest {

    @Mock
    private CatalogSyncService syncService;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private StartupSyncRunner runner;

    @Test
    void al_arrancar_pide_a_la_fachada_sincronizar_si_esta_vacio() {
        when(syncService.synchronizeIfEmpty()).thenReturn(true);

        runner.run(args);

        verify(syncService).synchronizeIfEmpty();
    }

    @Test
    void si_el_catalogo_ya_estaba_sincronizado_termina_sin_error() {
        when(syncService.synchronizeIfEmpty()).thenReturn(false);

        assertThatCode(() -> runner.run(args)).doesNotThrowAnyException();
    }

    @Test
    void si_la_catedra_no_esta_disponible_la_app_sigue_arrancando() {
        when(syncService.synchronizeIfEmpty())
                .thenThrow(new CatedraUnavailableException("cátedra caída", 503));

        assertThatCode(() -> runner.run(args)).doesNotThrowAnyException();
    }

    @Test
    void si_la_catedra_no_responde_sin_status_la_app_tambien_sigue_arrancando() {
        when(syncService.synchronizeIfEmpty())
                .thenThrow(new CatedraUnavailableException("timeout", new RuntimeException("read timed out")));

        assertThatCode(() -> runner.run(args)).doesNotThrowAnyException();
    }

    @Test
    void un_dato_invalido_en_el_snapshot_no_se_traga_y_hace_fallar_el_arranque() {
        InvalidCatalogDataException error = new InvalidCatalogDataException("snapshot corrupto");
        when(syncService.synchronizeIfEmpty()).thenThrow(error);

        assertThatThrownBy(() -> runner.run(args)).isSameAs(error);
    }
}
