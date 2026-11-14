package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalAgendaService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalAgendaResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalAgendaDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
@Tag(name = "Agenda", description = "Agenda vigente de un profesional, para que el servicio de turnos arme la disponibilidad")
public class ProfessionalAgendaController {

    private final ProfessionalAgendaService agendaService;
    private final ProfessionalAgendaDtoMapper mapper;

    @GetMapping("/{id}/agenda")
    @Operation(
            summary = "Agenda de un profesional",
            description = "Devuelve el profesional y todos sus horarios semanales, habilitados o no. Quien arme "
                    + "turnos debe ignorar los horarios con enabled=false. Se resuelve solo con datos locales.")
    @ApiResponse(responseCode = "200", description = "El profesional y sus horarios; la lista puede ser vacía")
    @ApiResponse(responseCode = "404",
            description = "No existe un profesional con ese ID (application/problem+json con code PROFESSIONAL_NOT_FOUND)")
    public ProfessionalAgendaResponse agenda(
            @Parameter(description = "ID del profesional en el catálogo de la cátedra", example = "201")
            @PathVariable Long id) {
        return mapper.toResponse(agendaService.getAgenda(id));
    }
}
