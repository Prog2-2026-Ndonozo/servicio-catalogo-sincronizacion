package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncState;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncStatus;

public interface SyncStatePort {
    void record(SyncStatus status, String detail);

    SyncState current();
}
