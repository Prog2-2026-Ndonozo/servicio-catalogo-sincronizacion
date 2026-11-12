package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalSearchService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
@Tag(name = "Profesionales", description = "Búsqueda de profesionales sobre la copia local del catálogo")
public class ProfessionalController {

    private final ProfessionalSearchService searchService;
    private final ProfessionalDtoMapper mapper;

    @GetMapping("/search")
    @Operation(
            summary = "Busca profesionales",
            description = "Aplica solo los filtros presentes y combina todos con AND. Sin filtros devuelve todos los "
                    + "profesionales, ordenados por apellido, nombre e id. Se resuelve siempre con datos locales.")
    @ApiResponse(responseCode = "200", description = "Lista de profesionales; vacía si ninguno coincide")
    @ApiResponse(responseCode = "400",
            description = "Parámetro inválido (application/problem+json con code VALIDATION_ERROR)")
    public List<ProfessionalResponse> search(
            @Parameter(description = "ID de la categoría del profesional. Debe ser positivo.", example = "101")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Texto que debe aparecer en el nombre o el apellido (o en ambos juntos). "
                    + "No distingue mayúsculas; sí distingue tildes. Máximo 100 caracteres.", example = "ana torres")
            @RequestParam(required = false) String name,
            @Parameter(description = "true: solo profesionales habilitados; false: solo deshabilitados.", example = "true")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Fecha (yyyy-MM-dd). Devuelve a quienes atienden ese día de la semana, es decir, "
                    + "con al menos un horario semanal habilitado. NO garantiza turnos libres: las ocupaciones las "
                    + "resuelve el servicio de turnos.", example = "2026-11-20")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableOn) {
        ProfessionalSearchCriteria criteria = mapper.toCriteria(categoryId, name, enabled, availableOn);
        return searchService.search(criteria).stream().map(mapper::toResponse).toList();
    }
}