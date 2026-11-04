package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CategoryDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.ProfessionalDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.WeeklyScheduleDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraSnapshotResponseRedisDto(
        long snapshotVersion,
        List<CategoryDto> professionalCategories,
        List<ProfessionalDto> professionals,
        List<WeeklyScheduleDto> weeklySchedules
) {
}
