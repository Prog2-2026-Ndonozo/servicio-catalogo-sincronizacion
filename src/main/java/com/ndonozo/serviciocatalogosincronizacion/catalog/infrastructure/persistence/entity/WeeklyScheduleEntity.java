package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

@Entity           // esto nos indica que es una entidad JPA
@Table(name= "weekly_schedules")  // a la tabla a la que apunta
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class WeeklyScheduleEntity {
    @Id

    private Long id;
    private Long professionalId;
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotDurationMinutes;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;



}
