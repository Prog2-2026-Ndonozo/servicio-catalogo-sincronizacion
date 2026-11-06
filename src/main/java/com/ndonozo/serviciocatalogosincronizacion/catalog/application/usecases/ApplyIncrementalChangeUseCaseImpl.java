package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogChanges;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyFullSnapshotUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ApplyIncrementalChangeUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatedraCatalogReadPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProcessedEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ApplyIncrementalChangeUseCaseImpl implements ApplyIncrementalChangeUseCase {

    private final CatedraCatalogReadPort catedra;
    private final CatalogRepositoryPort repository;
    private final ProcessedEventPort processedEvents;


    @Transactional
    @Override
    public CatalogVersion applyIncrementalChange(CatalogUpdate update){
        CatalogVersion local = repository.getCatalogVersion();

        // 1. Ya aplicado: la version local alcanza o supera a la del evento. Evento viejo termina sin llamar a redis
        if (update.getNewVersion() <= local.getSnapshotVersion()){
            return local;
        }
        // 2. Repetido: mismo eventId. Evento repetido termina aca sin llamar a Redis
        if (processedEvents.isProcessed(update.getEventId())){
            return local;

        }
        // 3.  juntar los IDs afectados desde la version siguiente a la local hasta dla del evento
        Set<Long> categoryIds = new LinkedHashSet<>();
        Set<Long> professionalIds = new LinkedHashSet<>();
        Set<Long> scheduleIds = new LinkedHashSet<>();
        for (long v = local.getSnapshotVersion() + 1 ; v <= update.getNewVersion();
             v++){
            CatalogChanges changes = catedra.fetchChanges(v);
            categoryIds.addAll(changes.getCategoryIds());
            professionalIds.addAll(changes.getProfessionalIds());
            scheduleIds.addAll(changes.getWeeklySchedulesIds());
        }

        //4. Leer el estado vigente de esas entidades y guardarlo (el orden no importa por las FK diferidas)

        repository.saveProfessionals(catedra.fetchProfessional(professionalIds));
        repository.saveWeeklySchedule(catedra.fetchWeeklySchedule(scheduleIds));
        repository.saveProfessionalCategories(catedra.fetchCategories(categoryIds));

        //5. La version y el eventId se registran al final, en la misma transaccion
        CatalogVersion applied = CatalogVersion.builder().snapshotVersion(update.getNewVersion()).build();
        repository.saveCatalogVersion(applied);
        processedEvents.markProcessed(update.getEventId(), update.getNewVersion());
        return applied;

    }





}
