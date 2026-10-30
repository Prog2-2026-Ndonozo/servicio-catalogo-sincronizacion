package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;

public interface ApplyIncrementalChangeUseCase {

    CatalogVersion applyIncrementalChange(CatalogUpdate update);

}
