package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.CatalogSyncService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.dto.CatalogUpdatedMessage;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.mapper.CatalogUpdatedMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogUpdatedListener {
    private final JsonMapper jsonMapper;
    private final CatalogUpdatedMapper mapper;
    private final CatalogSyncService syncService;

    @KafkaListener(topics="${catedra.catalog-topic}")
    public void onMessage(String payload){
        CatalogUpdate update ;
        try {
            CatalogUpdatedMessage message = jsonMapper.readValue(payload, CatalogUpdatedMessage.class);
            update = mapper.toDomain(message);
        } catch (JacksonException | InvalidCatalogDataException e) {
            log.warn("CatalogUpdated descartado : {}", e.getMessage());
            return;
        }
        log.info("CatalogUpdated recibido (eventId={}, newVersion={})",update.getEventId(),update.getNewVersion());
        syncService.applyCatalogUpdate(update);

    }
}
// Como funciona :  el mensaje llega como String,