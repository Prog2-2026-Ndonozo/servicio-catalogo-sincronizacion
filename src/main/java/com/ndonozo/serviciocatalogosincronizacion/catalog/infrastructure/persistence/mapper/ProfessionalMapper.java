package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;


import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalMapper {

    public Professional toDomain(ProfessionalEntity entity){
        if (entity == null){
            return null;
        }
        return Professional.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

    }
    public ProfessionalEntity toEntity(Professional professional){
        if (professional == null){
            return null;
        }
        ProfessionalEntity entity = new ProfessionalEntity();
        entity.setId(professional.getId());
        entity.setCategoryId(professional.getCategoryId());
        entity.setFirstName(professional.getFirstName());
        entity.setLastName(professional.getLastName());
        entity.setEnabled(professional.isEnabled());
        entity.setCreatedAt(professional.getCreatedAt());
        entity.setUpdatedAt(professional.getUpdatedAt());
        return entity;
    }
}

