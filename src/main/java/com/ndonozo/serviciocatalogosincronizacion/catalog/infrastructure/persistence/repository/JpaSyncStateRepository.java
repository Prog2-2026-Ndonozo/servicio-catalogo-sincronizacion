package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository;

import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.SyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSyncStateRepository extends JpaRepository<SyncStateEntity, Short> {
}
