package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalCategoryReadPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalCategoryMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JpaProfessionalCategoryReadAdapter implements ProfessionalCategoryReadPort {

    private static final Sort ORDER = Sort.by("name","id");

    private final JpaProfessionalCategoryRepository repository;
    private final ProfessionalCategoryMapper mapper;
    @Override
    public List<ProfessionalCategory> findAll(){
        return repository.findAll(ORDER).stream().map(mapper::toDomain).toList();
    }
}
