package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.*;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.CatalogChangesRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.CategoryRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.ProfessionalRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.WeeklyScheduleRedisDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

import static tools.jackson.databind.util.ClassUtil.name;

@Component
public class CatedraRedisMapper {

    public Professional toDomain(ProfessionalRedisDto dto) {
        return Professional.builder()
                .id(dto.id())
                .categoryId(dto.categoryId())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .enabled(dto.enabled())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .build();
    }

    public CatalogChanges toDomain(CatalogChangesRedisDto dto) {
        if (dto.changes() == null) {
            throw new InvalidCatalogDataException("changes must not be null for version " + dto.version());
        }
        return CatalogChanges.builder()
                .version(dto.version())
                .categoryIds(dto.changes().professionalCategories())
                .professionalIds(dto.changes().professionals())
                .weeklySchedulesIds(dto.changes().weeklySchedules())
                .build();
    }

    public VersionWindow toVersionWindow(Map<String, String> metadata) {
        return VersionWindow.builder()
                .currentVersion(parseVersion(metadata, "currentVersion"))
                .oldestAvailableVersion(parseVersion(metadata, "oldestAvailableVersion"))
                .build();
    }

    private static long parseVersion(Map<String, String> metadata, String field) {
        String value = metadata.get(field);
        if (value == null) {
            throw new InvalidCatalogDataException("Redis metadata is missing " + field);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidCatalogDataException("Redis metadata " + field + " is not a number: " + value);
        }
    }
    public ProfessionalCategory toDomain(CategoryRedisDto dto) {
        return ProfessionalCategory.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description() == null ? "" : dto.description())
                .enabled(dto.enabled())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .build();
    }
    public WeeklySchedule toDomain(WeeklyScheduleRedisDto dto){
        return WeeklySchedule.builder()
                .id(dto.id())
                .professionalId(dto.professionalId())
                .dayOfWeek(dto.dayOfWeek())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .slotDurationMinutes(dto.slotDurationMinutes())
                .enabled(dto.enabled())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .build();
    }
}