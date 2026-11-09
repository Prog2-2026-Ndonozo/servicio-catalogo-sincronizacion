package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;

import java.util.List;

/**
 * Persistencia de la copia local del catálogo.
 *
 * Los métodos de guardado son idempotentes: insertan o actualizan por ID,
 * de modo que aplicar dos veces el mismo cambio no altera el resultado (ADR-0018).
 *
 * No abre transacciones ni valida referencias entre colecciones: de eso se
 * encargan el caso de uso (la unidad de trabajo) y la base de datos
 * (claves foráneas diferidas).
 *
 * La versión aplicada no vive acá sino en {@link CatalogVersionPort}.
 */
public interface CatalogRepositoryPort {

    void saveProfessionalCategories(List<ProfessionalCategory> categories);

    void saveProfessionals(List<Professional> professionals);

    void saveWeeklySchedule(List<WeeklySchedule> weeklySchedules);
}
