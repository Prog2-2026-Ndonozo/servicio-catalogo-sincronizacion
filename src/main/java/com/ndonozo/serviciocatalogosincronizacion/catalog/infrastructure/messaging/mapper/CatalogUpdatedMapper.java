package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.dto.CatalogUpdatedMessage;
import org.springframework.stereotype.Component;

@Component
public class CatalogUpdatedMapper {

    private static final String EVENT_TYPE = "CatalogUpdated";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    public CatalogUpdate toDomain(CatalogUpdatedMessage message) {
        if ( message == null){
            throw new InvalidCatalogDataException("CatalogUpdate message is empty");
        }
        if (!EVENT_TYPE.equals(message.eventType())) {
            throw new InvalidCatalogDataException("Unexpected eventType: " + message.eventType());
        }
        if (message.schemaVersion() != SUPPORTED_SCHEMA_VERSION){
            throw new InvalidCatalogDataException("Unsupported schemaVersion: " + message.schemaVersion());
        }
        return CatalogUpdate.builder()
                .eventId(message.eventId())
                .newVersion(message.newVersion())
                .build();
    }

}
