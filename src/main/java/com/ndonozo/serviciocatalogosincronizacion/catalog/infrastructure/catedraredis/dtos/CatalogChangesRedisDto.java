package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogChangesRedisDto(long version, Changes changes) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Changes(
            List<Long> professionalCategories,
            List<Long> professional,
            List<Long> weeklySchedules
    ){

    }
}

// Chequear esto mejor