package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;

import java.util.List;

public interface SearchProfessionalsUseCase {

//    busca profesionales en la copia local, aplicando solo los filtros presentes
//            sin ningnun filtro devuelve todos. Como orden va apellido, nombre e id

    List<Professional> searchProfessionals(ProfessionalSearchCriteria criteria);
}
