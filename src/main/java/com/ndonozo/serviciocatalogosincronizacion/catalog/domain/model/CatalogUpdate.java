package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;


import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CatalogUpdate {
    private final String eventId;
    private final long newVersion;

    @Builder
    private CatalogUpdate(String eventId, long newVersion){
        this.eventId = requiredText(eventId, "eventId");
        this.newVersion = positive(newVersion, "newVersion");
    }
    private static String requiredText(String value, String field){
        if (value==null || value.isBlank()){
            throw new InvalidCatalogDataException("CatalogUpdate " + field + " must not be blank");

        }
        return value;
    }

    private static Long positive(long value, String field){
        if (value <= 0){
            throw new InvalidCatalogDataException("CatalogUpdate newVersion " + field + " must be positive " + value);
        }
        return value;
    }
}
