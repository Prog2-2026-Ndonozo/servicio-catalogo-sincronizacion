package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalCategoryMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalRepository;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaCatalogRepositoryAdapter implements CatalogRepositoryPort {
    private final JpaProfessionalRepository professionalRepository;
    private final JpaProfessionalCategoryRepository categoryRepository;
    private final JpaWeeklyScheduleRepository weeklySchedulesRepository;

    private final ProfessionalCategoryMapper categoryMapper;
    private final ProfessionalMapper professionalMapper;
    private final WeeklyScheduleMapper weeklyScheduleMapper;


    @Override
    public void saveProfessionalCategories(List<ProfessionalCategory> categories){
        categoryRepository.saveAll(categories.stream().map(categoryMapper::toEntity).toList());

    }

    @Override
    public void saveProfessionals(List<Professional> professionals){
        professionalRepository.saveAll(
                professionals.stream().map(professionalMapper::toEntity).toList()
        );
    }

    @Override
    public void saveWeeklySchedule(List<WeeklySchedule> weeklySchedules){
        weeklySchedulesRepository.saveAll(
                weeklySchedules.stream().map(weeklyScheduleMapper::toEntity).toList()
        );
    }
}
