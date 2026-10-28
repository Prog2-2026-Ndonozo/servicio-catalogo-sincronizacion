package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;

public interface ApplyFullSnapshotUseCase {
    /**
     * Obtiene el snapshot completo de la cátedra y reemplaza la copia local de forma consistente.
     * Devuelve la versión local que quedó aplicada.
     */

    CatalogVersion applyFullSnapshot();
}
