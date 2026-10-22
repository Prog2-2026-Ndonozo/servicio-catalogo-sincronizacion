package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository;

import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCatalogVersionRepository extends JpaRepository<CatalogVersionEntity, Short> {
}
