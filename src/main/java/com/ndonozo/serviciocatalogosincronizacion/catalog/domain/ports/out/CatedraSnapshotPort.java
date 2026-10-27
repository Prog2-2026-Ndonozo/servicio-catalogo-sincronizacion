package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogSnapshot;

public interface CatedraSnapshotPort {
    CatalogSnapshot fetchSnapshot();
}

// Obtiene de la catedra el snapshot completo del catalogo, con habilitados y deshabilitados