package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;

import java.util.List;

public interface ListProfessionalCategoriesUseCase {
    List<ProfessionalCategory> listProfessionalCategories();
}
