package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProcessedEventPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProcessedEventEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaProcessedEventAdapter implements ProcessedEventPort {

    private final JpaProcessedEventRepository repository;

    @Override
    public boolean isProcessed(String eventId){
        return repository.existsById(eventId);
    }
    @Override
    public void markProcessed(String eventId, long newVersion){
        repository.save(new ProcessedEventEntity(eventId,newVersion));
    }
}
