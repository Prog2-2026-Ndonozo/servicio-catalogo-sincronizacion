package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository;

import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaProfessionalRepository extends JpaRepository<ProfessionalEntity, Long>, JpaSpecificationExecutor<ProfessionalEntity> {
}
