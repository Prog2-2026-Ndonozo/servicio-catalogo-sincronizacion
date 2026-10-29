package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogSnapshot;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatedraSnapshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApplyFullSnapshotUseCaseImpl implements ApplyFullSnapshotUseCase {

    private final CatedraSnapshotPort snapshotPort;
    private final CatalogRepositoryPort repository;
    @Override
    @Transactional
    public CatalogVersion applyFullSnapshot(){
        CatalogSnapshot snapshot = snapshotPort.fetchSnapshot();
        repository.saveProfessionalCategories(snapshot.getCategories());   // el orden de esto no importa debido a las fk diferidas
        repository.saveProfessionals(snapshot.getProfessionals());
        repository.saveWeeklySchedule(snapshot.getWeeklySchedules());

        CatalogVersion applied = CatalogVersion.builder().snapshotVersion(snapshot.getSnapshotVersion()).build();
        repository.saveCatalogVersion(applied); // la version nunca avanza antes que los datos
        return applied;
    }

     //Aplica el snapshot en una sola transaccion: o se confirman las tres colecciones y la version o no queda nada

}
