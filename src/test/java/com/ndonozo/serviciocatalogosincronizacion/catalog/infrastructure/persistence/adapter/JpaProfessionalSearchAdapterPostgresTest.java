package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los cuatro filtros obligatorios (E §4.1) contra un PostgreSQL real. Cada test corre en una transaccion que se
 * revierte; las claves foraneas diferidas no se verifican porque nunca se hace commit.
 *
 * Datos:
 *   Ana Torres     cat 101  habilitada     lunes (habilitado), martes (deshabilitado)
 *   Mariana Gomez  cat 101  habilitada     miercoles (habilitado)
 *   Luis Torres    cat 102  deshabilitado  lunes (habilitado)
 *   Pedro Paz      cat 102  habilitado     sin horarios
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({JpaProfessionalSearchAdapter.class, ProfessionalMapper.class})
class JpaProfessionalSearchAdapterPostgresTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 10);
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 11);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 15);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaProfessionalSearchAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void datos() {
        categoria(101, "Clinica medica");
        categoria(102, "Pediatria");
        profesional(15, 101, "Ana", "Torres", true);
        profesional(16, 101, "Mariana", "Gomez", true);
        profesional(17, 102, "Luis", "Torres", false);
        profesional(18, 102, "Pedro", "Paz", true);
        horario(1, 15, "MONDAY", true);
        horario(2, 15, "TUESDAY", false);
        horario(3, 16, "WEDNESDAY", true);
        horario(4, 17, "MONDAY", true);
    }

    private void categoria(long id, String nombre) {
        jdbc.update("INSERT INTO professional_categories (id, name, description, enabled, created_at, updated_at) "
                + "VALUES (?, ?, '', true, now(), now())", id, nombre);
    }

    private void profesional(long id, long categoria, String nombre, String apellido, boolean habilitado) {
        jdbc.update("INSERT INTO professionals (id, category_id, first_name, last_name, enabled, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, now(), now())", id, categoria, nombre, apellido, habilitado);
    }

    private void horario(long id, long profesional, String dia, boolean habilitado) {
        jdbc.update("INSERT INTO weekly_schedules (id, professional_id, day_of_week, start_time, end_time, "
                + "slot_duration_minutes, enabled, created_at, updated_at) "
                + "VALUES (?, ?, ?, '09:00', '12:00', 30, ?, now(), now())", id, profesional, dia, habilitado);
    }

    private List<Long> ids(ProfessionalSearchCriteria criteria) {
        return adapter.search(criteria).stream().map(Professional::getId).toList();
    }

    private static ProfessionalSearchCriteria.ProfessionalSearchCriteriaBuilder criterios() {
        return ProfessionalSearchCriteria.builder();
    }

    // ---------- sin filtros y orden ----------

    @Test
    void sin_filtros_devuelve_todos_ordenados_por_apellido_nombre_e_id() {
        // Gomez, Paz, Torres Ana, Torres Luis
        assertThat(ids(criterios().build())).containsExactly(16L, 18L, 15L, 17L);
    }

    // ---------- categoria ----------

    @Test
    void filtra_por_categoria() {
        assertThat(ids(criterios().categoryId(101L).build())).containsExactly(16L, 15L);
        assertThat(ids(criterios().categoryId(102L).build())).containsExactly(18L, 17L);
    }

    @Test
    void una_categoria_inexistente_no_devuelve_nada() {
        assertThat(ids(criterios().categoryId(999L).build())).isEmpty();
    }

    // ---------- nombre ----------

    @Test
    void el_nombre_busca_dentro_del_nombre_y_no_distingue_mayusculas() {
        // "ana" esta en "Ana" y en "Mariana"
        assertThat(ids(criterios().name("ana").build())).containsExactly(16L, 15L);
        assertThat(ids(criterios().name("ANA").build())).containsExactly(16L, 15L);
    }

    @Test
    void el_nombre_tambien_busca_en_el_apellido() {
        assertThat(ids(criterios().name("torres").build())).containsExactly(15L, 17L);
    }

    @Test
    void el_nombre_busca_sobre_nombre_y_apellido_juntos() {
        assertThat(ids(criterios().name("ana torres").build())).containsExactly(15L);
    }

    @Test
    void el_orden_apellido_nombre_no_coincide() {
        assertThat(ids(criterios().name("torres ana").build())).isEmpty();
    }

    @Test
    void un_nombre_con_espacios_sobrantes_se_recorta() {
        assertThat(ids(criterios().name("  ana torres  ").build())).containsExactly(15L);
    }

    @Test
    void los_comodines_del_usuario_se_buscan_literalmente() {
        assertThat(ids(criterios().name("%").build())).isEmpty();
        assertThat(ids(criterios().name("_").build())).isEmpty();
        assertThat(ids(criterios().name("a_a").build())).isEmpty();
    }

    @Test
    void una_barra_invertida_se_busca_literalmente() {
        assertThat(ids(criterios().name("\\").build())).isEmpty();
    }

    @Test
    void un_comodin_se_encuentra_si_el_nombre_realmente_lo_contiene() {
        profesional(19, 101, "Jo%e", "Perez", true);

        assertThat(ids(criterios().name("jo%e").build())).containsExactly(19L);
    }

    // ---------- habilitado ----------

    @Test
    void filtra_por_habilitado() {
        assertThat(ids(criterios().enabled(true).build())).containsExactly(16L, 18L, 15L);
        assertThat(ids(criterios().enabled(false).build())).containsExactly(17L);
    }

    // ---------- disponibilidad ----------

    @Test
    void disponible_un_lunes_devuelve_a_quienes_tienen_horario_habilitado_ese_dia() {
        assertThat(ids(criterios().availableOn(LUNES).build())).containsExactly(15L, 17L);
    }

    @Test
    void un_horario_deshabilitado_no_cuenta_como_disponibilidad() {
        // Ana tiene el martes pero esta deshabilitado
        assertThat(ids(criterios().availableOn(MARTES).build())).isEmpty();
    }

    @Test
    void disponible_un_miercoles() {
        assertThat(ids(criterios().availableOn(MIERCOLES).build())).containsExactly(16L);
    }

    @Test
    void un_dia_sin_horarios_no_devuelve_nada() {
        assertThat(ids(criterios().availableOn(DOMINGO).build())).isEmpty();
    }

    @Test
    void la_disponibilidad_depende_del_dia_de_la_semana_y_no_de_la_fecha_exacta() {
        assertThat(ids(criterios().availableOn(LUNES.plusWeeks(5)).build())).containsExactly(15L, 17L);
    }

    @Test
    void un_profesional_con_varios_horarios_el_mismo_dia_no_se_repite() {
        horario(5, 15, "MONDAY", true);

        assertThat(ids(criterios().availableOn(LUNES).build())).containsExactly(15L, 17L);
    }

    @Test
    void la_disponibilidad_es_independiente_del_estado_del_profesional() {
        // Luis esta deshabilitado pero tiene horario habilitado el lunes
        assertThat(ids(criterios().availableOn(LUNES).build())).contains(17L);
    }

    // ---------- combinaciones ----------

    @Test
    void los_filtros_se_combinan_todos_a_la_vez() {
        assertThat(ids(criterios().categoryId(101L).name("torres").enabled(true).availableOn(LUNES).build()))
                .containsExactly(15L);
    }

    @Test
    void habilitado_y_disponible_el_lunes_excluye_al_deshabilitado() {
        assertThat(ids(criterios().enabled(true).availableOn(LUNES).build())).containsExactly(15L);
    }

    @Test
    void una_combinacion_sin_coincidencias_devuelve_vacio() {
        assertThat(ids(criterios().categoryId(102L).name("ana").build())).isEmpty();
    }

    @Test
    void devuelve_el_profesional_completo_mapeado_al_dominio() {
        Professional ana = adapter.search(criterios().name("ana torres").build()).getFirst();

        assertThat(ana.getId()).isEqualTo(15L);
        assertThat(ana.getCategoryId()).isEqualTo(101L);
        assertThat(ana.getFirstName()).isEqualTo("Ana");
        assertThat(ana.getLastName()).isEqualTo("Torres");
        assertThat(ana.isEnabled()).isTrue();
    }
}
