package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
public class CatalogChanges {
    private final long version;
    private final List<Long> categoryIds;
    private final List<Long> professionalIds;
    private final List<Long> weeklySchedulesIds;

    @Builder
    private CatalogChanges(Long version, List<Long> categoryIds, List<Long> professionalIds, List<Long> weeklySchedulesIds ) {
        if (version <= 0){
            throw new InvalidCatalogDataException("CatalogChanges version must be positive " + version);}
        this.version = version;
        this.categoryIds = validIds(categoryIds, "categoryIds");
        this.professionalIds = validIds(professionalIds, "professionalIds");
        this.weeklySchedulesIds = validIds(weeklySchedulesIds, "weeklySchedulesIds");
    }
    private static List<Long>validIds(List<Long> ids, String field){
        if (ids == null){
            throw new InvalidCatalogDataException("CatalogChanges " + field + " must not be null");

        }
        for (Long id : ids){
            if (id == null || id <= 0){
                throw new InvalidCatalogDataException("CatalogChanges " + field + " has an invalid id " + id);
            }
        }
        return List.copyOf(ids);


    }

}
