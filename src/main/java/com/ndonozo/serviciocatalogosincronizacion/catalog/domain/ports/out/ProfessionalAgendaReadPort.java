package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;

import java.util.Optional;

public interface ProfessionalAgendaReadPort {

    /** El profesional y todos sus horarios, ordenados por dia de la semana, hora de inicio e id. */
    Optional<ProfessionalAgenda> findByProfessionalId(long professionalId);
}
