package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.WeeklyScheduleMapper;
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

import java.time.DayOfWeek;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La consulta de horarios por profesional y el orden por dia de la semana, contra un PostgreSQL real. Cada test corre
 * en una transaccion que se revierte.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({JpaProfessionalAgendaReadAdapter.class, ProfessionalMapper.class, WeeklyScheduleMapper.class})
class JpaProfessionalAgendaReadAdapterPostgresTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private JpaProfessionalAgendaReadAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void datos() {
        jdbc.update("INSERT INTO professional_categories (id, name, description, enabled, created_at, updated_at) "
                + "VALUES (101, 'Cardiologia', '', true, now(), now())");
        profesional(201, "Ana", "Torres", true);
        profesional(202, "Luis", "Paz", false);
        profesional(203, "Sin", "Horarios", true);
        // insertados desordenados a proposito
        horario(1, 201, "FRIDAY", "09:00", true);
        horario(2, 201, "MONDAY", "14:00", true);
        horario(3, 201, "WEDNESDAY", "09:00", false);
        horario(4, 201, "MONDAY", "09:00", true);
        horario(5, 202, "TUESDAY", "09:00", true);
    }

    private void profesional(long id, String nombre, String apellido, boolean habilitado) {
        jdbc.update("INSERT INTO professionals (id, category_id, first_name, last_name, enabled, created_at, updated_at) "
                + "VALUES (?, 101, ?, ?, ?, now(), now())", id, nombre, apellido, habilitado);
    }

    private void horario(long id, long profesional, String dia, String inicio, boolean habilitado) {
        jdbc.update("INSERT INTO weekly_schedules (id, professional_id, day_of_week, start_time, end_time, "
                + "slot_duration_minutes, enabled, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?::time, ?::time + interval '3 hours', 30, ?, now(), now())",
                id, profesional, dia, inicio, inicio, habilitado);
    }

    @Test
    void devuelve_el_profesional_y_todos_sus_horarios_ordenados_por_dia_y_hora() {
        ProfessionalAgenda agenda = adapter.findByProfessionalId(201).orElseThrow();

        assertThat(agenda.professional().getFirstName()).isEqualTo("Ana");
        // lunes 09:00, lunes 14:00, miercoles, viernes (alfabeticamente empezaria el viernes)
        assertThat(agenda.weeklySchedules()).extracting(WeeklySchedule::getId).containsExactly(4L, 2L, 3L, 1L);
        assertThat(agenda.weeklySchedules()).extracting(WeeklySchedule::getDayOfWeek)
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void incluye_los_horarios_deshabilitados_con_su_indicador() {
        List<WeeklySchedule> horarios = adapter.findByProfessionalId(201).orElseThrow().weeklySchedules();

        assertThat(horarios).filteredOn(h -> !h.isEnabled()).extracting(WeeklySchedule::getId).containsExactly(3L);
        assertThat(horarios).filteredOn(WeeklySchedule::isEnabled).hasSize(3);
    }

    @Test
    void no_mezcla_los_horarios_de_otro_profesional() {
        List<WeeklySchedule> horarios = adapter.findByProfessionalId(202).orElseThrow().weeklySchedules();

        assertThat(horarios).extracting(WeeklySchedule::getId).containsExactly(5L);
    }

    @Test
    void un_profesional_deshabilitado_tambien_tiene_agenda() {
        assertThat(adapter.findByProfessionalId(202).orElseThrow().professional().isEnabled()).isFalse();
    }

    @Test
    void un_profesional_sin_horarios_devuelve_una_agenda_vacia() {
        assertThat(adapter.findByProfessionalId(203).orElseThrow().weeklySchedules()).isEmpty();
    }

    @Test
    void un_profesional_inexistente_devuelve_vacio() {
        assertThat(adapter.findByProfessionalId(999)).isEmpty();
    }
}
