package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CatedraCatalogReadPort {
    // Version vigente y la mas antigua con cambios disponibles
    VersionWindow fetchVersionWindow();

    // Id's afectados por una version. Lanza InvalidCatalogDataException si no existe
    Optional<CatalogChanges> fetchChanges(long version);


    List<ProfessionalCategory> fetchCategories(Collection<Long> ids);
    List<Professional> fetchProfessional(Collection<Long> ids );
    List<WeeklySchedule> fetchWeeklySchedule(Collection<Long> ids);
}
