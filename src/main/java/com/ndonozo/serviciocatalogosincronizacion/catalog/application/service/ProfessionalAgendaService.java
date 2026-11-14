package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.exception.ProfessionalNotFoundException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.GetProfessionalAgendaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfessionalAgendaService {

    private final GetProfessionalAgendaUseCase getProfessionalAgenda;

    public ProfessionalAgenda getAgenda(long professionalId) {
        return getProfessionalAgenda.getProfessionalAgenda(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
    }
}
