package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalAgendaResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.WeeklyScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfessionalAgendaDtoMapper {

    private final ProfessionalDtoMapper professionalMapper;

    public ProfessionalAgendaResponse toResponse(ProfessionalAgenda agenda) {
        if (agenda == null) {
            return null;
        }
        return new ProfessionalAgendaResponse(
                professionalMapper.toResponse(agenda.professional()),
                agenda.weeklySchedules().stream().map(this::toResponse).toList());
    }

    private WeeklyScheduleResponse toResponse(WeeklySchedule schedule) {
        return new WeeklyScheduleResponse(schedule.getId(), schedule.getDayOfWeek(), schedule.getStartTime(),
                schedule.getEndTime(), schedule.getSlotDurationMinutes(), schedule.isEnabled());
    }
}
