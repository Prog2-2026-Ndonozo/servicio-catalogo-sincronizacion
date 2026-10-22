package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import org.springframework.stereotype.Component;

@Component
public class CatalogVersionMapper {


    public CatalogVersion toDomain(CatalogVersionEntity entity){
        if(entity == null){
            return null;
        }
        return CatalogVersion.builder()
                .snapshotVersion(entity.getSnapshotVersion())
                .build();
    }
    public CatalogVersionEntity toEntity(CatalogVersion catalogVersion){
        if (catalogVersion == null){
            return null;
        }

        CatalogVersionEntity entity = new CatalogVersionEntity();
        entity.setSnapshotVersion(catalogVersion.getSnapshotVersion());
        entity.setId(CatalogVersionEntity.SINGLE_ROW_ID);
        return entity;
    }

}
