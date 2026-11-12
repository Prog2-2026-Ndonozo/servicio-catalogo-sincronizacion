package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.ListProfessionalCategoriesUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalCategoryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListProfessionalCategoriesUseCaseImpl implements ListProfessionalCategoriesUseCase {
    private final ProfessionalCategoryReadPort readPort;
    @Override
    public List<ProfessionalCategory> listProfessionalCategories(){
        return readPort.findAll();
    }
}
