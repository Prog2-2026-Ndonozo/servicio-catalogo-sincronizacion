package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.SearchProfessionalsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalSearchService {
    private final SearchProfessionalsUseCase searchProfessionals;

    public List<Professional> search(ProfessionalSearchCriteria criteria){
        return searchProfessionals.searchProfessionals(criteria);

    }
}
