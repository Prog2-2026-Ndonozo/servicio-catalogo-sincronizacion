package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba contra un PostgreSQL real (contenedor descartable, misma imagen que compose.yaml).
 * Flyway aplica las migraciones y Hibernate corre en ddl-auto=validate, asi que tambien se comprueba
 * que la entidad calza con la tabla (V2) y que el DEFAULT now() de processed_at funciona.
 * Cada test corre en una transaccion que se revierte: no queda nada entre un test y otro.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaProcessedEventAdapter.class)
class JpaProcessedEventAdapterPostgresTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaProcessedEventAdapter adapter;

    @Autowired
    private JpaProcessedEventRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void un_evento_nunca_registrado_no_figura_como_procesado() {
        assertThat(adapter.isProcessed("catalog-event-7-4f3b")).isFalse();
    }

    @Test
    void un_evento_registrado_figura_como_procesado() {
        adapter.markProcessed("catalog-event-7-4f3b", 7);

        assertThat(adapter.isProcessed("catalog-event-7-4f3b")).isTrue();
    }

    @Test
    void registrar_un_evento_no_marca_a_otros() {
        adapter.markProcessed("catalog-event-7-4f3b", 7);

        assertThat(adapter.isProcessed("catalog-event-8-aaaa")).isFalse();
    }

    @Test
    void se_guarda_la_version_del_evento() {
        adapter.markProcessed("catalog-event-7-4f3b", 7);
        repository.flush();

        Long version = jdbc.queryForObject(
                "SELECT new_version FROM processed_events WHERE event_id = ?", Long.class, "catalog-event-7-4f3b");
        assertThat(version).isEqualTo(7L);
    }

    @Test
    void la_base_completa_processed_at_con_su_valor_por_defecto() {
        adapter.markProcessed("catalog-event-7-4f3b", 7);
        repository.flush();

        OffsetDateTime procesado = jdbc.queryForObject(
                "SELECT processed_at FROM processed_events WHERE event_id = ?", OffsetDateTime.class,
                "catalog-event-7-4f3b");
        assertThat(procesado).isNotNull().isAfter(OffsetDateTime.now().minusMinutes(1));
    }

    @Test
    void registrar_dos_veces_el_mismo_evento_deja_una_sola_fila() {
        adapter.markProcessed("catalog-event-7-4f3b", 7);
        adapter.markProcessed("catalog-event-7-4f3b", 7);
        repository.flush();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events", Long.class)).isEqualTo(1L);
    }

    @Test
    void la_base_rechaza_una_version_no_positiva() {
        adapter.markProcessed("catalog-event-0", 0);

        assertThatThrownBy(repository::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void la_base_rechaza_un_event_id_en_blanco() {
        adapter.markProcessed("   ", 7);

        assertThatThrownBy(repository::flush).isInstanceOf(DataIntegrityViolationException.class);
    }
}
