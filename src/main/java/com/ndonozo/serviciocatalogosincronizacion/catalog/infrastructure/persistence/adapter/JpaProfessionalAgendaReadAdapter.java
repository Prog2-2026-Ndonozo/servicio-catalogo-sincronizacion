package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalAgendaReadPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalRepository;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaProfessionalAgendaReadAdapter implements ProfessionalAgendaReadPort {

    /** El dia se ordena como DayOfWeek (lunes primero), no como texto: la base lo guarda como "FRIDAY", "MONDAY"... */
    private static final Comparator<WeeklySchedule> ORDEN = Comparator.comparing(WeeklySchedule::getDayOfWeek)
            .thenComparing(WeeklySchedule::getStartTime)
            .thenComparing(WeeklySchedule::getId);

    private final JpaProfessionalRepository professionalRepository;
    private final JpaWeeklyScheduleRepository scheduleRepository;
    private final ProfessionalMapper professionalMapper;
    private final WeeklyScheduleMapper scheduleMapper;

    @Override
    public Optional<ProfessionalAgenda> findByProfessionalId(long professionalId) {
        return professionalRepository.findById(professionalId).map(entity -> {
            List<WeeklySchedule> schedules = scheduleRepository.findByProfessionalId(professionalId).stream()
                    .map(scheduleMapper::toDomain)
                    .sorted(ORDEN)
                    .toList();
            return new ProfessionalAgenda(professionalMapper.toDomain(entity), schedules);
        });
    }
}
