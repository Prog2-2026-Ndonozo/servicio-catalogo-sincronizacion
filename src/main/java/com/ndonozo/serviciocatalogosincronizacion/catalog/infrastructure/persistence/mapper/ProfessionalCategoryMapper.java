package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import org.springframework.stereotype.Component;

@Component

public class ProfessionalCategoryMapper {

    public ProfessionalCategory toDomain(ProfessionalCategoryEntity entity){
        if (entity == null){
            return null;
        }
        return ProfessionalCategory.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

    }

    public ProfessionalCategoryEntity toEntity(ProfessionalCategory category){
        if (category == null){
            return null;
        }
         ProfessionalCategoryEntity entity = new ProfessionalCategoryEntity();
        entity.setId(category.getId());
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
        entity.setEnabled(category.isEnabled());
        entity.setCreatedAt(category.getCreatedAt());
        entity.setUpdatedAt(category.getUpdatedAt());
        return entity;
    }

}
