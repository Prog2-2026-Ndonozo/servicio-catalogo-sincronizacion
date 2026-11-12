package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProfessionalDtoMapper {

    public ProfessionalSearchCriteria toCriteria(Long categoryId, String name, Boolean enabeld, LocalDate availableOn){
        try {
            return ProfessionalSearchCriteria.builder().categoryId(categoryId).name(name).enabled(enabeld).availableOn(availableOn).build();
        }catch(InvalidCatalogDataException e ){
            throw new InvalidSearchRequestException(e.getMessage());
        }

    }
    public ProfessionalResponse toResponse(Professional professional){
        if (professional == null){
            return null;
        }
        return new ProfessionalResponse(professional.getId(),professional.getCategoryId(),professional.getFirstName(),professional.getLastName(),professional.isEnabled());

    }
}
