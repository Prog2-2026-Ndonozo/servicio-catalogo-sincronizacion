package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.FullSyncRequiredException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncState;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncStatus;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyIncrementalChangeUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogVersionPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.SyncStatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.lang.classfile.Attributes.record;

import static org.springframework.data.redis.connection.Limit.limit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogSyncService {

    private static final int MAX_DETAIL = 500;

    private final ApplyFullSnapshotUseCase applyFullSnapshot;
    private final ApplyIncrementalChangeUseCase applyIncrementalChange;
    private final CatalogVersionPort catalogVersion;
    private final SyncStatePort syncState;

    public boolean synchronizeIfEmpty() {
        if (catalogVersion.getCatalogVersion().getSnapshotVersion() > 0) {
            return false;                      // ya sincronizado: no se escribe nada
        }
        try {
            applyFullSnapshot.applyFullSnapshot();
        } catch (RuntimeException e) {
            recordError(e);
            throw e;
        }
        record(SyncStatus.OK, null);
        return true;
    }

    public CatalogVersion applyCatalogUpdate(CatalogUpdate update) {
        long before = catalogVersion.getCatalogVersion().getSnapshotVersion();
        try {
            CatalogVersion applied = applyIncrementalChange.applyIncrementalChange(update);
            if (applied.getSnapshotVersion() > before) {   // si el evento se ignoró, la versión no cambió
                record(SyncStatus.OK, null);
            }
            return applied;
        } catch (FullSyncRequiredException e) {
            log.warn("Sincronizacion incremental no posible ({}). Se pide el snapshot completo.", e.getMessage());
            try {
                CatalogVersion rebuilt = applyFullSnapshot.applyFullSnapshot();
                record(SyncStatus.RECONSTRUIDO_POR_DISCONTINUIDAD, limit(e.getMessage()));
                return rebuilt;
            } catch (RuntimeException failure) {
                recordError(failure);
                throw failure;
            }
        } catch (RuntimeException e) {
            recordError(e);
            throw e;
        }
    }

    private void record(SyncStatus status, String detail) {
        try {
            syncState.record(status, detail);
        } catch (RuntimeException e) {
            // el estado es diagnóstico: un fallo al registrarlo no cambia el resultado de la sincronización
            log.warn("No se pudo registrar el estado de sincronizacion {}: {}", status, e.getClass().getSimpleName());
        }
    }

    private void recordError(RuntimeException e) {
        record(SyncStatus.ERROR, limit(e.getClass().getSimpleName() + ": " + e.getMessage()));
    }

    private static String limit(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_DETAIL ? text.substring(0, MAX_DETAIL) : text;
    }
}
