package com.ndonozo.serviciocatalogosincronizacion;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalCategoryService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller.ProfessionalCategoryController;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalCategoryDtoMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.security.ProblemJsonSecurityHandlers;
import com.ndonozo.serviciocatalogosincronizacion.config.SecurityConfig;
import com.ndonozo.serviciocatalogosincronizacion.support.JwtTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el servidor de recursos con el decodificador real que arma Spring Boot a partir de las propiedades (clave
 * publica, emisor y audiencia), no con el post-procesador jwt() de Spring Security, que lo saltearia (ADR-0037).
 */
@WebMvcTest(ProfessionalCategoryController.class)
@Import({SecurityConfig.class, ProblemJsonSecurityHandlers.class, ProfessionalCategoryDtoMapper.class})
class ApiSecurityTest extends JwtTestSupport {

    private static final String URL = "/api/professional-categories";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessionalCategoryService categoryService;

    @BeforeEach
    void servicio() {
        when(categoryService.list()).thenReturn(List.of());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- token valido ----------

    @Test
    void un_token_valido_de_turnos_con_el_permiso_de_lectura_accede() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(status().isOk());
    }

    @Test
    void un_token_con_el_usuario_en_nombre_de_quien_actua_turnos_tambien_accede() throws Exception {
        String token = token().enNombreDe("user-42").build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    // ---------- 401 ----------

    @Test
    void sin_token_responde_401_problem_json_con_codigo_y_cabecera_bearer() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void un_token_firmado_con_otra_clave_responde_401() throws Exception {
        String token = token().firmadoPor(CLAVES_AJENAS).build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void un_token_vencido_responde_401() throws Exception {
        String token = token().vencidoHace(600).build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void un_token_que_todavia_no_es_valido_responde_401() throws Exception {
        String token = token().validoDesdeDentroDe(600).build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void un_token_de_otro_emisor_responde_401() throws Exception {
        String token = token().emisor("otro-servicio").build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void un_token_para_otra_audiencia_responde_401() throws Exception {
        String token = token().audiencia("otro-destinatario").build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void un_token_mal_formado_responde_401() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer("esto-no-es-un-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otro_esquema_de_autorizacion_responde_401() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 403 ----------

    @Test
    void un_token_valido_sin_el_permiso_responde_403_problem_json_con_codigo() throws Exception {
        String token = token().permiso(null).build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void un_token_valido_con_otro_permiso_responde_403() throws Exception {
        String token = token().permiso("otro:permiso").build();

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    // ---------- lo que no es una ruta de datos ----------

    @Test
    void una_ruta_fuera_de_api_se_rechaza_aun_con_un_token_valido() throws Exception {
        mockMvc.perform(get("/otra-ruta").header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(status().isForbidden());
    }

    @Test
    void la_documentacion_interactiva_esta_cerrada_fuera_del_perfil_de_desarrollo() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
    }
}
