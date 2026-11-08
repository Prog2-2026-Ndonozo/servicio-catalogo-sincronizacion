package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogChangesRedisDto(long version, Changes changes) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Changes(
            List<Long> professionalCategories,
            List<Long> professionals,
            List<Long> weeklySchedules
    ){

    }
}

// Chequear esto mejor