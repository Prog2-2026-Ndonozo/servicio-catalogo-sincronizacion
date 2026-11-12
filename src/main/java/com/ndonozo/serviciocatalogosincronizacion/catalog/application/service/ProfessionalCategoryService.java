package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ListProfessionalCategoriesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalCategoryService {
    private final ListProfessionalCategoriesUseCase listProfessionalCategories;

    public List<ProfessionalCategory> list(){
        return listProfessionalCategories.listProfessionalCategories();
    }
}
