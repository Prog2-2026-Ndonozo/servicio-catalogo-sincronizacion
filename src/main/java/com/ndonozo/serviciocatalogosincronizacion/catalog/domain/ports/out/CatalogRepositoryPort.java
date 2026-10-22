package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;

import java.util.List;

public interface CatalogRepositoryPort {

    void saveProfessionalCategories(List<ProfessionalCategory> categories)  ;

    void saveProfessionals(List<Professional> profesional);

    void saveWeeklySchedule(List<WeeklySchedule> weeklySchedules);

    CatalogVersion getCatalogVersion();
    void saveCatalogVersion(CatalogVersion version);


}


/**
 * Persistencia de la copia local del catálogo y de su versión aplicada.
 *
 * Los métodos de guardado son idempotentes: insertan o actualizan por ID,
 * de modo que aplicar dos veces el mismo cambio no altera el resultado (ADR-0018).
 *
 * No abre transacciones ni valida referencias entre colecciones: de eso se
 * encargan el caso de uso (la unidad de trabajo) y la base de datos
 * (claves foráneas diferidas).
 */

/**
 * CatalogVersion getCatalogVersion()  Devuelve la versión del catálogo aplicada localmente.
 * Nunca es null: en una base sin sincronizar devuelve la versión 0 (ADR-0014).
 */

