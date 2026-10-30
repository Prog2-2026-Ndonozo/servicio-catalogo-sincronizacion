package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyIncrementalChangeUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CatalogSyncService {
    private final ApplyFullSnapshotUseCase applyFullSnapshot;
    private final ApplyIncrementalChangeUseCase applyIncrementalSnapshot;
    private final CatalogRepositoryPort repository;

    public boolean synchronizeIfEmpty(){
        if (repository.getCatalogVersion().getSnapshotVersion() > 0){
            return false;
        }
        applyFullSnapshot.applyFullSnapshot();
        return true;
    }
//    public CatalogVersion applyCatalogUpdate(CatalogUpdate update){
//        //return applyIncrementalChange.applyIncrementalCHange(update);
//    }

}
