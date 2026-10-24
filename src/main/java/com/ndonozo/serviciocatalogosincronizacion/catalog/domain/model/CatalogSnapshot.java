package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class CatalogSnapshot {
    private final long snapshotVersion;
    private final List<Professional> professionals;
    private final List<ProfessionalCategory> categories;
    private final List<WeeklySchedule> weeklySchedules;

    @Builder
    private CatalogSnapshot(long snapshotVersion, List<Professional> professionals, List<ProfessionalCategory> categories, List<WeeklySchedule> weeklySchedules) {

        // Que eel snapshotversion no sea negativo
        // que la lista no sea nulla, y guardar List.copyoff
    }

}
