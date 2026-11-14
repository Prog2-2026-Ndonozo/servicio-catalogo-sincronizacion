package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.GetProfessionalAgendaUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalAgendaReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProfessionalAgendaUseCaseImpl implements GetProfessionalAgendaUseCase {

    private final ProfessionalAgendaReadPort readPort;

    @Override
    public Optional<ProfessionalAgenda> getProfessionalAgenda(long professionalId) {
        return readPort.findByProfessionalId(professionalId);
    }
}
