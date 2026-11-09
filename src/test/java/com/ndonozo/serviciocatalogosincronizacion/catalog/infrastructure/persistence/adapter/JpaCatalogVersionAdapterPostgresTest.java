package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.CatalogVersionMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaCatalogVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra un PostgreSQL real: comprueba que la fila unica de catalog_version existe tras la migracion V1
 * (version 0, ADR-0014) y que guardar y leer la version funciona de punta a punta.
 * Cada test corre en una transaccion que se revierte.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({JpaCatalogVersionAdapter.class, CatalogVersionMapper.class})
class JpaCatalogVersionAdapterPostgresTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaCatalogVersionAdapter adapter;

    @Autowired
    private JpaCatalogVersionRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void una_base_recien_migrada_devuelve_la_version_cero() {
        assertThat(adapter.getCatalogVersion().getSnapshotVersion()).isZero();
    }

    @Test
    void guardar_y_leer_devuelve_la_version_guardada() {
        adapter.saveCatalogVersion(CatalogVersion.builder().snapshotVersion(55).build());
        repository.flush();

        assertThat(adapter.getCatalogVersion().getSnapshotVersion()).isEqualTo(55L);
    }

    @Test
    void guardar_varias_veces_actualiza_la_misma_fila() {
        adapter.saveCatalogVersion(CatalogVersion.builder().snapshotVersion(3).build());
        adapter.saveCatalogVersion(CatalogVersion.builder().snapshotVersion(4).build());
        repository.flush();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM catalog_version", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT snapshot_version FROM catalog_version", Long.class)).isEqualTo(4L);
    }
}
