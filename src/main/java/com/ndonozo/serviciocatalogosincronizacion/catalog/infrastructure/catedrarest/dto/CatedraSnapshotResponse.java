package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto;

import java.util.List;

public record CatedraSnapshotResponse(
        long snapshotVersion,
        List<CategoryDto> professionalCategories,
        List<ProfessionalDto> professionals,
        List<WeeklyScheduleDto> weeklySchedules
) {
}
