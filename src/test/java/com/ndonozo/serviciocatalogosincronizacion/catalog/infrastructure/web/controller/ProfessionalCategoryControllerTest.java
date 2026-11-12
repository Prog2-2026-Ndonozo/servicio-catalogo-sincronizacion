package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalCategoryService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalCategoryDtoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfessionalCategoryController.class)
@Import(ProfessionalCategoryDtoMapper.class)
class ProfessionalCategoryControllerTest {

    private static final String URL = "/api/professional-categories";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessionalCategoryService categoryService;

    private static ProfessionalCategory categoria(long id, String nombre, String descripcion, boolean enabled) {
        return ProfessionalCategory.builder().id(id).name(nombre).description(descripcion).enabled(enabled)
                .createdAt(Instant.parse("2026-07-01T12:00:00Z")).updatedAt(Instant.parse("2026-09-10T13:40:00Z")).build();
    }

    @Test
    void responde_200_con_las_categorias_en_el_orden_de_la_fachada() throws Exception {
        when(categoryService.list()).thenReturn(List.of(
                categoria(101, "Clinica medica", "Atencion clinica general", true),
                categoria(102, "Pediatria", "", false)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].name").value("Clinica medica"))
                .andExpect(jsonPath("$[0].description").value("Atencion clinica general"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[1].id").value(102))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    @Test
    void no_expone_las_fechas_de_creacion_ni_de_actualizacion() throws Exception {
        when(categoryService.list()).thenReturn(List.of(categoria(101, "Clinica medica", "", true)));

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());
    }

    @Test
    void sin_categorias_responde_200_con_una_lista_vacia() throws Exception {
        when(categoryService.list()).thenReturn(List.of());

        mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void solo_se_acepta_get() throws Exception {
        mockMvc.perform(post(URL)).andExpect(status().isMethodNotAllowed());
    }
}
