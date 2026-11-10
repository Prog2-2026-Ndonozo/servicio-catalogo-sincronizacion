package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncState;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.SyncStatus;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.SyncStatePort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.SyncStateEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaSyncStateAdapter implements SyncStatePort {
    private final JpaSyncStateRepository repository;

    @Override
    @Transactional
    public void record(SyncStatus status, String detail){
        SyncStateEntity entity = row();
        entity.setStatus(status);
        entity.setDetail(detail);
        repository.save(entity);
    }
    @Override
    @Transactional (readOnly = true)
    public SyncState current(){
        SyncStateEntity entity = row();
        return new SyncState(entity.getStatus(), entity.getDetail(),entity.getUpdatedAt());

    }
    private SyncStateEntity row(){
        return repository.findById(SyncStateEntity.SINGLE_ROW_ID).orElseThrow(()-> new IllegalStateException("sync_state has no row: the migration did not run"));

    }
}
