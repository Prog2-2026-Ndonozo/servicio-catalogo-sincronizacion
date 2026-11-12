package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalSearchService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalSearchService searchService;
    private final ProfessionalDtoMapper mapper;

    @GetMapping("/search")
    public List<ProfessionalResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableOn){
        ProfessionalSearchCriteria criteria = mapper.toCriteria(categoryId,name,enabled,availableOn);
        return searchService.search(criteria).stream().map(mapper::toResponse).toList();
    };

}
