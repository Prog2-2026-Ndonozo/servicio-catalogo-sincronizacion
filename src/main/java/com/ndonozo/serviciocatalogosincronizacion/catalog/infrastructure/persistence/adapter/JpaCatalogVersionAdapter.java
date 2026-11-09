package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogVersionPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.CatalogVersionMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaCatalogVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaCatalogVersionAdapter implements CatalogVersionPort {

    private final JpaCatalogVersionRepository repository;
    private final CatalogVersionMapper mapper;

    @Override
    public CatalogVersion getCatalogVersion() {
        return repository.findById(CatalogVersionEntity.SINGLE_ROW_ID)
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "catalog_version has no row: the initial migration did not run"));
    }

    @Override
    public void saveCatalogVersion(CatalogVersion version) {
        repository.save(mapper.toEntity(version));
    }
}
