package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogSnapshot;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatedraSnapshotPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.client.CatedraRestClient;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.mapper.SnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatedraSnapshotRestAdapter implements CatedraSnapshotPort {
    private final CatedraRestClient client;
    private final SnapshotMapper mapper;

    @Override
    public CatalogSnapshot fetchSnapshot(){
        return mapper.toDomain(client.fetchSnapshot());
    }
}
