package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalCategoryService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalCategoryResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalCategoryDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/professional-categories")
@RequiredArgsConstructor
public class ProfessionalCategoryController {
    private final ProfessionalCategoryService categoryService;
    private final ProfessionalCategoryDtoMapper mapper;

    @GetMapping
    public List<ProfessionalCategoryResponse> list(){
        return categoryService.list().stream().map(mapper::toResponse).toList();
    }
}
