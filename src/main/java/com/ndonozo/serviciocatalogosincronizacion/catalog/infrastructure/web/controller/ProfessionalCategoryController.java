package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalCategoryService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.dto.ProfessionalCategoryResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalCategoryDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/professional-categories")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Busqueda de categorias sobre la copia local del catalogo")
public class ProfessionalCategoryController {
    private final ProfessionalCategoryService categoryService;
    private final ProfessionalCategoryDtoMapper mapper;
    @Operation(
            summary = "lista categorias",
            description = "Devuelve todas las categorias, esten habilitadas o no, ordenadas por nombre e id. Sirve para " +
            " ofrecer el filtro por categoria de la busqueda."

    )
    @ApiResponse(responseCode = "200", description = "Lista de categorias; vacia si no hay ninguna")


    @GetMapping
    public List<ProfessionalCategoryResponse> list(){
        return categoryService.list().stream().map(mapper::toResponse).toList();
    }
}
