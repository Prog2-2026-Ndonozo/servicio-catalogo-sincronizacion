package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;

import java.util.Optional;

public interface GetProfessionalAgendaUseCase {

    /** Agenda vigente de un profesional, o vacio si no existe. Se resuelve solo con datos locales. */
    Optional<ProfessionalAgenda> getProfessionalAgenda(long professionalId);
}
