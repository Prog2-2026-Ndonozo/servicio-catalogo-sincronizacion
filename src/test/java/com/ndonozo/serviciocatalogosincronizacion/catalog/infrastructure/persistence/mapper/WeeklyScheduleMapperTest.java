package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;

class WeeklyScheduleMapperTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    private final WeeklyScheduleMapper mapper = new WeeklyScheduleMapper();

    private static WeeklySchedule horario() {
        return WeeklySchedule.builder()
                .id(21L)
                .professionalId(15L)
                .dayOfWeek(MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .enabled(false)
                .createdAt(CREADO)
                .updatedAt(ACTUALIZADO)
                .build();
    }

    @Test
    void toEntity_copia_cada_campo_a_su_columna() {
        WeeklyScheduleEntity entity = mapper.toEntity(horario());

        assertThat(entity.getId()).isEqualTo(21L);
        assertThat(entity.getProfessionalId()).isEqualTo(15L);
        assertThat(entity.getDayOfWeek()).isEqualTo(MONDAY);
        assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(entity.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(entity.isEnabled()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(CREADO);
        assertThat(entity.getUpdatedAt()).isEqualTo(ACTUALIZADO);
    }

    @Test
    void ida_y_vuelta_conserva_todos_los_campos() {
        WeeklySchedule original = horario();

        WeeklySchedule resultado = mapper.toDomain(mapper.toEntity(original));

        assertThat(resultado).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void null_devuelve_null_en_ambos_sentidos() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
