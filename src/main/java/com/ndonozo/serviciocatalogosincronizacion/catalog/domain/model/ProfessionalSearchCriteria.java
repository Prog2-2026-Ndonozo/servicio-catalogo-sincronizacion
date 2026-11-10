package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter

public class ProfessionalSearchCriteria {
    private static final int MAX_NAME_LENGHT = 100;

    private final Long categoryId;
    private final String name;
    private final Boolean enabled;
    private final LocalDate availableOn;

    @Builder
    private ProfessionalSearchCriteria(Long categoryId, String name, Boolean enabled, LocalDate availableOn){
        this.categoryId = positiveOrNull(categoryId);
        this.name = normalizedName(name);
        this.enabled = enabled;
        this.availableOn = availableOn;
    }

    private static Long positiveOrNull(Long id ){
        if (id != null && id <= 0){
            throw  new InvalidCatalogDataException("ProfessionalSearchCriteria categoryId must be positive " + id);
        }
        return id;

    }
    private static String normalizedName(String name){
        if (name == null || name.isBlank()){
            return null;
        }
        String trimmed = name.strip();
        if (trimmed.length() > MAX_NAME_LENGHT){
            throw new InvalidCatalogDataException("ProfessionalSearchCriteria name must not exceed " + MAX_NAME_LENGHT);

        }
        return trimmed;
    }
}
