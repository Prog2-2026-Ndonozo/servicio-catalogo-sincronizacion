package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogSnapshot;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CatedraSnapshotResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CategoryDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.ProfessionalDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.WeeklyScheduleDto;
import org.springframework.stereotype.Component;

@Component
public class SnapshotMapper {

    public CatalogSnapshot toDomain(CatedraSnapshotResponse response) {
        return CatalogSnapshot.builder()
                .snapshotVersion(response.snapshotVersion())
                .categories(response.professionalCategories().stream().map(this::toDomain).toList())
                .professionals(response.professionals().stream().map(this::toDomain).toList())
                .weeklySchedules(response.weeklySchedules().stream().map(this::toDomain).toList())
                .build();
    }

    private ProfessionalCategory toDomain(CategoryDto dto) {
        return ProfessionalCategory.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description() == null ? "" : dto.description())
                .enabled(dto.enabled())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .build();
    }

    private Professional toDomain(ProfessionalDto dto) {
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

    private WeeklySchedule toDomain(WeeklyScheduleDto dto) {
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
