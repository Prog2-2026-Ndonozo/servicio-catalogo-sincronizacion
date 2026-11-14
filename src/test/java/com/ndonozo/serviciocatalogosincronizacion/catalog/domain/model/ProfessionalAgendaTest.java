package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfessionalAgendaTest {

    private static final Instant CREADO = Instant.parse("2026-09-01T03:00:00Z");

    private static Professional profesional(long id) {
        return Professional.builder().id(id).categoryId(101L).firstName("Ana").lastName("Torres").enabled(true)
                .createdAt(CREADO).updatedAt(CREADO).build();
    }

    private static WeeklySchedule horario(long id, long professionalId) {
        return WeeklySchedule.builder().id(id).professionalId(professionalId).dayOfWeek(MONDAY)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).slotDurationMinutes(30).enabled(true)
                .createdAt(CREADO).updatedAt(CREADO).build();
    }

    @Test
    void una_agenda_valida_conserva_el_profesional_y_sus_horarios() {
        ProfessionalAgenda agenda = new ProfessionalAgenda(profesional(201), List.of(horario(1, 201), horario(2, 201)));

        assertThat(agenda.professional().getId()).isEqualTo(201L);
        assertThat(agenda.weeklySchedules()).extracting(WeeklySchedule::getId).containsExactly(1L, 2L);
    }

    @Test
    void un_profesional_sin_horarios_tiene_una_agenda_valida_y_vacia() {
        assertThat(new ProfessionalAgenda(profesional(201), List.of()).weeklySchedules()).isEmpty();
    }

    @Test
    void la_lista_de_horarios_es_inmutable_y_no_depende_de_la_original() {
        List<WeeklySchedule> original = new ArrayList<>(List.of(horario(1, 201)));
        ProfessionalAgenda agenda = new ProfessionalAgenda(profesional(201), original);

        original.add(horario(2, 201));

        assertThat(agenda.weeklySchedules()).hasSize(1);
        assertThatThrownBy(() -> agenda.weeklySchedules().add(horario(3, 201)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void sin_profesional_se_rechaza() {
        assertThatThrownBy(() -> new ProfessionalAgenda(null, List.of()))
                .isInstanceOf(InvalidCatalogDataException.class).hasMessageContaining("professional");
    }

    @Test
    void sin_lista_de_horarios_se_rechaza() {
        assertThatThrownBy(() -> new ProfessionalAgenda(profesional(201), null))
                .isInstanceOf(InvalidCatalogDataException.class).hasMessageContaining("weeklySchedules");
    }

    @Test
    void un_horario_de_otro_profesional_se_rechaza() {
        assertThatThrownBy(() -> new ProfessionalAgenda(profesional(201), List.of(horario(1, 201), horario(2, 999))))
                .isInstanceOf(InvalidCatalogDataException.class).hasMessageContaining("another professional");
    }

    @Test
    void un_horario_nulo_se_rechaza() {
        assertThatThrownBy(() -> new ProfessionalAgenda(profesional(201), Arrays.asList(horario(1, 201), null)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }
}
