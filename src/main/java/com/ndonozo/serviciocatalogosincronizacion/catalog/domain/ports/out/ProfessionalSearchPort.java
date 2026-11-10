package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;

import java.util.List;

public interface ProfessionalSearchPort {

    // Resuelve la busqueda sobre la copia local, nunca consultamos a la catedra

    List<Professional> search(ProfessionalSearchCriteria criteria);
}
