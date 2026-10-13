package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Duration;

@Getter
public class WeeklySchedule {

    private final Long id;
    private final Long professionalId;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int slotDurationMinutes;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    private WeeklySchedule(Long id, Long professionalId, DayOfWeek dayOfWeek, LocalTime startTime,
                           LocalTime endTime, int slotDurationMinutes, boolean enabled,
                           Instant createdAt, Instant updatedAt) {
        this.id = required(id, "id");
        this.professionalId = required(professionalId, "professionalId");
        this.dayOfWeek = required(dayOfWeek, "dayOfWeek");
        this.startTime = required(startTime, "startTime");
        this.endTime = required(endTime, "endTime");
        validateTimeRange(this.startTime,this.endTime);
        this.slotDurationMinutes = slotDurationMinutes;
        validateSlotDuration(this.startTime,this.endTime,this.slotDurationMinutes);
        this.enabled = enabled;
        this.createdAt = required(createdAt, "createdAt");
        this.updatedAt = required(updatedAt, "updatedAt");
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidCatalogDataException("WeeklySchedule " + field + " must not be null");
        }
        return value;
    }

    private static void validateTimeRange(LocalTime startTime, LocalTime endTime){
        if (!startTime.isBefore(endTime)){
            throw new InvalidCatalogDataException(
                    "WeeklySchedule startTime must be before endTime"
            );
        }

    }
    private static void validateSlotDuration(LocalTime startTime, LocalTime endTime, int slotDurationMinutes){
        if (slotDurationMinutes <= 0 ){
            throw new InvalidCatalogDataException("WeeklySchedule slotDurationMinutes must be greater than 0");

        }
        long rangeMinutes = Duration.between(startTime,endTime).toMinutes();
        if (rangeMinutes <= 0 || rangeMinutes % slotDurationMinutes != 0){       // Importante: duration.toMinutes() trunca segundos y haria que un rango de menos de un minut pase el test, por eso decidimos <= 0
            throw new InvalidCatalogDataException("WeeklySchedule slotDurationMinutes must divide the schedule range");
        } //ligado al test de rangos menores a un minuto
    }
}

