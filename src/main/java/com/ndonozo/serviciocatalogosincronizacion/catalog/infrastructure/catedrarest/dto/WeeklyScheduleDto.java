package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record WeeklyScheduleDto(
        Long id,
        Long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
