package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProfessionalAgendaResponse(
        @Schema(description = "El profesional, con su indicador enabled") ProfessionalResponse professional,
        @Schema(description = "Todos sus horarios semanales, habilitados o no, por día de la semana y hora de inicio")
        List<WeeklyScheduleResponse> weeklySchedules) {
}
