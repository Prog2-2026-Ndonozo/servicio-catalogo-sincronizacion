package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto;

public record ProfessionalResponse(Long id, Long categoryId, String firstName, String lastName, boolean enabled) {
}
