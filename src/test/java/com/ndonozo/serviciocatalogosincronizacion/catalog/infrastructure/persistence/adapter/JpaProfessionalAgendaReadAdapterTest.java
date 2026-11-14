package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalRepository;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProfessionalAgendaReadAdapterTest {

    private static final Instant FECHA = Instant.parse("2026-09-01T03:00:00Z");

    @Mock
    private JpaProfessionalRepository professionalRepository;

    @Mock
    private JpaWeeklyScheduleRepository scheduleRepository;

    private JpaProfessionalAgendaReadAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaProfessionalAgendaReadAdapter(professionalRepository, scheduleRepository,
                new ProfessionalMapper(), new WeeklyScheduleMapper());
    }

    private static ProfessionalEntity profesional(long id) {
        ProfessionalEntity e = new ProfessionalEntity();
        e.setId(id);
        e.setCategoryId(101L);
        e.setFirstName("Ana");
        e.setLastName("Torres");
        e.setEnabled(true);
        e.setCreatedAt(FECHA);
        e.setUpdatedAt(FECHA);
        return e;
    }

    private static WeeklyScheduleEntity horario(long id, long professionalId, DayOfWeek dia, int hora, boolean enabled) {
        WeeklyScheduleEntity e = new WeeklyScheduleEntity();
        e.setId(id);
        e.setProfessionalId(professionalId);
        e.setDayOfWeek(dia);
        e.setStartTime(LocalTime.of(hora, 0));
        e.setEndTime(LocalTime.of(hora + 3, 0));
        e.setSlotDurationMinutes(30);
        e.setEnabled(enabled);
        e.setCreatedAt(FECHA);
        e.setUpdatedAt(FECHA);
        return e;
    }

    @Test
    void arma_la_agenda_con_el_profesional_y_sus_horarios_mapeados_al_dominio() {
        when(professionalRepository.findById(201L)).thenReturn(Optional.of(profesional(201)));
        when(scheduleRepository.findByProfessionalId(201L))
                .thenReturn(List.of(horario(1, 201, DayOfWeek.MONDAY, 9, false)));

        ProfessionalAgenda agenda = adapter.findByProfessionalId(201).orElseThrow();

        assertThat(agenda.professional().getId()).isEqualTo(201L);
        assertThat(agenda.professional().getLastName()).isEqualTo("Torres");
        assertThat(agenda.weeklySchedules()).hasSize(1);
        assertThat(agenda.weeklySchedules().getFirst().isEnabled()).isFalse();
    }

    @Test
    void ordena_por_dia_de_la_semana_y_no_alfabeticamente() {
        when(professionalRepository.findById(201L)).thenReturn(Optional.of(profesional(201)));
        when(scheduleRepository.findByProfessionalId(201L)).thenReturn(List.of(
                horario(1, 201, DayOfWeek.FRIDAY, 9, true),
                horario(2, 201, DayOfWeek.MONDAY, 14, true),
                horario(3, 201, DayOfWeek.WEDNESDAY, 9, true),
                horario(4, 201, DayOfWeek.MONDAY, 9, true)));

        List<WeeklySchedule> horarios = adapter.findByProfessionalId(201).orElseThrow().weeklySchedules();

        // alfabeticamente quedaria FRIDAY primero: el orden correcto empieza el lunes
        assertThat(horarios).extracting(WeeklySchedule::getId).containsExactly(4L, 2L, 3L, 1L);
    }

    @Test
    void un_profesional_sin_horarios_devuelve_una_agenda_con_lista_vacia() {
        when(professionalRepository.findById(201L)).thenReturn(Optional.of(profesional(201)));
        when(scheduleRepository.findByProfessionalId(201L)).thenReturn(List.of());

        assertThat(adapter.findByProfessionalId(201).orElseThrow().weeklySchedules()).isEmpty();
    }

    @Test
    void un_profesional_inexistente_devuelve_vacio_y_no_consulta_los_horarios() {
        when(professionalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(adapter.findByProfessionalId(999)).isEmpty();
        verify(scheduleRepository, never()).findByProfessionalId(999L);
    }
}
