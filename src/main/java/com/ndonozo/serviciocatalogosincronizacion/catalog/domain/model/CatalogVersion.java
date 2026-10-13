package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;


import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;


@Getter
public class CatalogVersion {
    private final long snapshotVersion;




    @Builder
    private CatalogVersion(long snapshotVersion){
        this.snapshotVersion = nonNegative(snapshotVersion, "snapshotVersion");


    }
    private static long nonNegative(long value, String field){
        if (value < 0){
            throw new InvalidCatalogDataException("CatalogVersion " + field + " can't be negative" + value);

        }
        return value;
    }

}
