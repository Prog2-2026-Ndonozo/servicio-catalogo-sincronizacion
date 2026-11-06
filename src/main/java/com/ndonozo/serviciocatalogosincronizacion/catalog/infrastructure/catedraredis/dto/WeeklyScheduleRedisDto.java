package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeeklyScheduleRedisDto(
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
