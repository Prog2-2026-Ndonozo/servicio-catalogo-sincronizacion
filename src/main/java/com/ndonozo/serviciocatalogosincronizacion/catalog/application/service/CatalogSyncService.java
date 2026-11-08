package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.FullSyncRequiredException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyIncrementalChangeUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogSyncService {
    private final ApplyFullSnapshotUseCase applyFullSnapshot;
    private final ApplyIncrementalChangeUseCase applyIncrementalChange;
    private final CatalogRepositoryPort repository;

    public boolean synchronizeIfEmpty(){
        if (repository.getCatalogVersion().getSnapshotVersion() > 0){
            return false;
        }
        applyFullSnapshot.applyFullSnapshot();
        return true;
    }
   public CatalogVersion applyCatalogUpdate(CatalogUpdate update){
        try {
            return applyIncrementalChange.applyIncrementalChange(update);

        } catch (FullSyncRequiredException e){
            log.warn("Sincronizacion incremental no posible ({}). Se pide el snapshot completo.", e.getMessage());
            return applyFullSnapshot.applyFullSnapshot();
        }
    }

}
