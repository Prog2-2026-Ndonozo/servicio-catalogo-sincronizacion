package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.stereotype.Component;

@Component
public class WeeklyScheduleMapper {

    public WeeklySchedule toDomain(WeeklyScheduleEntity entity) {
        if (entity == null) {
            return null;
        }
        return WeeklySchedule.builder()
                .id(entity.getId())
                .professionalId(entity.getProfessionalId())
                .dayOfWeek(entity.getDayOfWeek())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .slotDurationMinutes(entity.getSlotDurationMinutes())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public WeeklyScheduleEntity toEntity(WeeklySchedule schedule) {
        if (schedule == null) {
            return null;
        }
        WeeklyScheduleEntity entity = new WeeklyScheduleEntity();
        entity.setId(schedule.getId());
        entity.setProfessionalId(schedule.getProfessionalId());
        entity.setDayOfWeek(schedule.getDayOfWeek());
        entity.setStartTime(schedule.getStartTime());
        entity.setEndTime(schedule.getEndTime());
        entity.setSlotDurationMinutes(schedule.getSlotDurationMinutes());
        entity.setEnabled(schedule.isEnabled());
        entity.setCreatedAt(schedule.getCreatedAt());
        entity.setUpdatedAt(schedule.getUpdatedAt());
        return entity;
    }
}
