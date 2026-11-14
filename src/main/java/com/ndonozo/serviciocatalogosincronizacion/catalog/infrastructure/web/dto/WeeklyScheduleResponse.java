package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklyScheduleResponse(
        @Schema(description = "ID del horario en el catálogo de la cátedra", example = "21") Long id,
        @Schema(description = "Día de la semana", example = "MONDAY") DayOfWeek dayOfWeek,
        @Schema(description = "Hora de inicio de la atención (HH:mm:ss)", example = "09:00:00") LocalTime startTime,
        @Schema(description = "Hora de fin de la atención (HH:mm:ss)", example = "12:00:00") LocalTime endTime,
        @Schema(description = "Duración de cada turno, en minutos", example = "30") int slotDurationMinutes,
        @Schema(description = "false si el horario está dado de baja: no debe usarse para ofrecer turnos",
                example = "true") boolean enabled) {
}
