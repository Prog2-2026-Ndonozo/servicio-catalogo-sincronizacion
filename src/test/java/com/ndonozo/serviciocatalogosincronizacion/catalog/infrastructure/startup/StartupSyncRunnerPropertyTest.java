package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.startup;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.CatalogSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StartupSyncRunnerPropertyTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(CatalogSyncService.class, () -> mock(CatalogSyncService.class))
            .withUserConfiguration(StartupSyncRunner.class);

    @Test
    void sin_la_propiedad_el_runner_esta_activo() {
        contexto.run(ctx -> assertThat(ctx).hasSingleBean(StartupSyncRunner.class));
    }

    @Test
    void con_la_propiedad_en_true_el_runner_esta_activo() {
        contexto.withPropertyValues("catedra.sync-on-startup=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(StartupSyncRunner.class));
    }

    @Test
    void con_la_propiedad_en_false_el_runner_no_se_crea() {
        contexto.withPropertyValues("catedra.sync-on-startup=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(StartupSyncRunner.class));
    }
}
