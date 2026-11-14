package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalAgendaResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.WeeklyScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalAgendaDtoMapperTest {

    private static final Instant FECHA = Instant.parse("2026-09-01T03:00:00Z");

    private final ProfessionalAgendaDtoMapper mapper = new ProfessionalAgendaDtoMapper(new ProfessionalDtoMapper());

    private static Professional profesional() {
        return Professional.builder().id(201L).categoryId(101L).firstName("Ana").lastName("Torres").enabled(true)
                .createdAt(FECHA).updatedAt(FECHA).build();
    }

    private static WeeklySchedule horario(long id, boolean enabled) {
        return WeeklySchedule.builder().id(id).professionalId(201L).dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 30)).slotDurationMinutes(30).enabled(enabled)
                .createdAt(FECHA).updatedAt(FECHA).build();
    }

    @Test
    void copia_el_profesional_y_todos_los_campos_de_cada_horario() {
        ProfessionalAgendaResponse response = mapper.toResponse(
                new ProfessionalAgenda(profesional(), List.of(horario(21, true))));

        assertThat(response.professional().id()).isEqualTo(201L);
        assertThat(response.professional().lastName()).isEqualTo("Torres");
        assertThat(response.weeklySchedules()).containsExactly(
                new WeeklyScheduleResponse(21L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 30), 30, true));
    }

    @Test
    void conserva_los_horarios_deshabilitados_y_su_orden() {
        ProfessionalAgendaResponse response = mapper.toResponse(
                new ProfessionalAgenda(profesional(), List.of(horario(1, true), horario(2, false))));

        assertThat(response.weeklySchedules()).extracting(WeeklyScheduleResponse::id).containsExactly(1L, 2L);
        assertThat(response.weeklySchedules().get(1).enabled()).isFalse();
    }

    @Test
    void una_agenda_sin_horarios_da_una_lista_vacia() {
        assertThat(mapper.toResponse(new ProfessionalAgenda(profesional(), List.of())).weeklySchedules()).isEmpty();
    }

    @Test
    void con_null_devuelve_null() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
