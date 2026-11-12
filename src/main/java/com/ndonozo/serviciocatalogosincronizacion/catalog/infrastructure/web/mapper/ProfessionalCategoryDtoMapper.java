package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalCategoryDtoMapper {
    public ProfessionalCategoryResponse toResponse(ProfessionalCategory category){
        if (category == null){
            return null;
        }
        return new ProfessionalCategoryResponse(category.getId(),category.getName(), category.getDescription(), category.isEnabled());
    }
}
