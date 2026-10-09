package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;


import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;


@Getter
public class ProfessionalCategory {
    private final Long id;
    private final String name;
    private final String description;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;


    @Builder
    private ProfessionalCategory(Long id,String name, String description, boolean enabled, Instant createdAt, Instant updatedAt){
        this.id =required(id,"id");
        this.name = notBlank(name,"name");
        this.description=required(description,"description");
        this.enabled = enabled;
        this.createdAt = required(createdAt, "createdAt");
        this.updatedAt = required(updatedAt, "updatedAt");


    }
    private static <T> T required(T value, String field){
        if (value == null){
            throw new InvalidCatalogDataException("ProfessionalCategory " + field + " must not be null");

        }
        return value;
    }
    private static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidCatalogDataException("ProfessionalCategory " + field + " must not be null or blank");
        }
        return value;
    }

}
