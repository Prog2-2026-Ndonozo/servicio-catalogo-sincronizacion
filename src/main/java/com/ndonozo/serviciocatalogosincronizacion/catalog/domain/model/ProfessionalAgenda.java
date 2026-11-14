package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;

import java.util.List;

/** Un profesional junto con todos sus horarios semanales, habilitados o no (el indicador enabled decide). */
public record ProfessionalAgenda(Professional professional, List<WeeklySchedule> weeklySchedules) {

    public ProfessionalAgenda {
        if (professional == null) {
            throw new InvalidCatalogDataException("ProfessionalAgenda professional must not be null");
        }
        if (weeklySchedules == null) {
            throw new InvalidCatalogDataException("ProfessionalAgenda weeklySchedules must not be null");
        }
        for (WeeklySchedule schedule : weeklySchedules) {
            if (schedule == null || !professional.getId().equals(schedule.getProfessionalId())) {
                throw new InvalidCatalogDataException("ProfessionalAgenda has a schedule of another professional: "
                        + (schedule == null ? null : schedule.getId()));
            }
        }
        weeklySchedules = List.copyOf(weeklySchedules);
    }
}
