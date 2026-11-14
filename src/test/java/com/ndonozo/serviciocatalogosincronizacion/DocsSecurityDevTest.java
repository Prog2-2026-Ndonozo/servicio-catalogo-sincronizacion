package com.ndonozo.serviciocatalogosincronizacion;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalCategoryService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller.ProfessionalCategoryController;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalCategoryDtoMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.security.ProblemJsonSecurityHandlers;
import com.ndonozo.serviciocatalogosincronizacion.config.SecurityConfig;
import com.ndonozo.serviciocatalogosincronizacion.support.JwtTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con el perfil dev, la documentacion interactiva se abre sin token; los endpoints de datos siguen protegidos. Como el
 * corte de MVC no carga springdoc, una ruta abierta responde 404 (no hay manejador) y no 401.
 */
@WebMvcTest(ProfessionalCategoryController.class)
@Import({SecurityConfig.class, ProblemJsonSecurityHandlers.class, ProfessionalCategoryDtoMapper.class})
@ActiveProfiles("dev")
class DocsSecurityDevTest extends JwtTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessionalCategoryService categoryService;

    @Test
    void en_desarrollo_la_documentacion_no_pide_token() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void en_desarrollo_los_endpoints_de_datos_siguen_protegidos() throws Exception {
        mockMvc.perform(get("/api/professional-categories")).andExpect(status().isUnauthorized());
    }
}
