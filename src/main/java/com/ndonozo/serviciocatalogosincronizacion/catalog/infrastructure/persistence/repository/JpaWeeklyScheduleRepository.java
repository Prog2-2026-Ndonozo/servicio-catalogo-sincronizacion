package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository;

import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaWeeklyScheduleRepository extends JpaRepository<WeeklyScheduleEntity, Long> {

    List<WeeklyScheduleEntity> findByProfessionalId(Long professionalId);
}
