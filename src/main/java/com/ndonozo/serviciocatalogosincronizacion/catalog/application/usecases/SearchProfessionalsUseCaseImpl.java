package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.SearchProfessionalsUseCase;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchProfessionalsUseCaseImpl implements SearchProfessionalsUseCase {


    private final ProfessionalSearchPort searchPort;

    @Override
    public List<Professional> searchProfessionals(ProfessionalSearchCriteria criteria){
        return searchPort.search(criteria);
    }
}
