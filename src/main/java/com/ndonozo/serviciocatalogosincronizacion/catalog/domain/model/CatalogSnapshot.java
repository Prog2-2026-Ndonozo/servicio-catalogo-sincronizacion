package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
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
    private CatalogSnapshot(long snapshotVersion, List<Professional> professionals,
                            List<ProfessionalCategory> categories, List<WeeklySchedule> weeklySchedules) {
        this.snapshotVersion = nonNegative(snapshotVersion, "snapshotVersion");
        this.professionals = List.copyOf(required(professionals, "professionals"));
        this.categories = List.copyOf(required(categories, "categories"));
        this.weeklySchedules = List.copyOf(required(weeklySchedules, "weeklySchedules"));
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidCatalogDataException("CatalogSnapshot " + field + " must not be null");
        }
        return value;
    }

    private static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new InvalidCatalogDataException("CatalogSnapshot " + field + " can't be negative: " + value);
        }
        return value;
    }
}
