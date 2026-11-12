package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;


import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalSearchPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.ProfessionalSpecifications;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;



@Component
@RequiredArgsConstructor
public class JpaProfessionalSearchAdapter implements ProfessionalSearchPort {
    private static final Sort ORDER = Sort.by("lastName","firstName", "id");
    private final JpaProfessionalRepository repository;
    private final ProfessionalMapper mapper ;


    @Override
    public List<Professional> search(ProfessionalSearchCriteria criteria){
        return repository.findAll(ProfessionalSpecifications.from(criteria),ORDER).stream().map(mapper::toDomain).toList();
    }
}
