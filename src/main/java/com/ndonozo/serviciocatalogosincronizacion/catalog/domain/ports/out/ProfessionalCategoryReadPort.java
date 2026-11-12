package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;

import java.util.List;

public interface ProfessionalCategoryReadPort {
    List<ProfessionalCategory> findAll();

    // Todas las categorias de la copia local, habilitadas o no, ordenadas por nombre e id
}
