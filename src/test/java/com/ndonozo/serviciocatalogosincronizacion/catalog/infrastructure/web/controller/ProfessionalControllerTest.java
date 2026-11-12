package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalSearchService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.exception.ApiExceptionHandler;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalDtoMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el adaptador de entrada de la busqueda con la fachada simulada: de los parametros HTTP a los criterios,
 * de los resultados al JSON y de las solicitudes invalidas a 400. No hay seguridad todavia (#10).
 */
@WebMvcTest(ProfessionalController.class)
@Import({ProfessionalDtoMapper.class, ApiExceptionHandler.class})
class ProfessionalControllerTest {

    private static final String URL = "/api/professionals/search";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessionalSearchService searchService;

    private static Professional profesional(long id, String nombre, String apellido, boolean enabled) {
        return Professional.builder().id(id).categoryId(101L).firstName(nombre).lastName(apellido).enabled(enabled)
                .createdAt(Instant.parse("2026-09-01T03:00:00Z")).updatedAt(Instant.parse("2026-11-06T14:19:29Z"))
                .build();
    }

    private ProfessionalSearchCriteria criteriosRecibidos() {
        ArgumentCaptor<ProfessionalSearchCriteria> captor = ArgumentCaptor.forClass(ProfessionalSearchCriteria.class);
        verify(searchService).search(captor.capture());
        return captor.getValue();
    }

    // ---------- respuesta ----------

    @Test
    void sin_parametros_busca_sin_filtros_y_responde_200_con_la_lista() throws Exception {
        when(searchService.search(any())).thenReturn(List.of(profesional(15, "Ana", "Torres", true)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[0].categoryId").value(101))
                .andExpect(jsonPath("$[0].firstName").value("Ana"))
                .andExpect(jsonPath("$[0].lastName").value("Torres"))
                .andExpect(jsonPath("$[0].enabled").value(true));

        ProfessionalSearchCriteria criteria = criteriosRecibidos();
        assertThat(criteria.getCategoryId()).isNull();
        assertThat(criteria.getName()).isNull();
        assertThat(criteria.getEnabled()).isNull();
        assertThat(criteria.getAvailableOn()).isNull();
    }

    @Test
    void la_respuesta_no_expone_las_fechas_de_creacion_ni_de_actualizacion() throws Exception {
        when(searchService.search(any())).thenReturn(List.of(profesional(15, "Ana", "Torres", true)));

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());
    }

    @Test
    void sin_resultados_responde_200_con_una_lista_vacia() throws Exception {
        when(searchService.search(any())).thenReturn(List.of());

        mockMvc.perform(get(URL).param("name", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void conserva_el_orden_que_devuelve_la_fachada() throws Exception {
        when(searchService.search(any())).thenReturn(List.of(
                profesional(16, "Mariana", "Gomez", true),
                profesional(15, "Ana", "Torres", false)));

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[0].id").value(16))
                .andExpect(jsonPath("$[1].id").value(15))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    // ---------- parametros ----------

    @Test
    void los_cuatro_parametros_llegan_a_los_criterios() throws Exception {
        when(searchService.search(any())).thenReturn(List.of());

        mockMvc.perform(get(URL)
                        .param("categoryId", "101")
                        .param("name", "ana")
                        .param("enabled", "true")
                        .param("availableOn", "2026-11-20"))
                .andExpect(status().isOk());

        ProfessionalSearchCriteria criteria = criteriosRecibidos();
        assertThat(criteria.getCategoryId()).isEqualTo(101L);
        assertThat(criteria.getName()).isEqualTo("ana");
        assertThat(criteria.getEnabled()).isTrue();
        assertThat(criteria.getAvailableOn()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void enabled_false_llega_como_false_y_no_como_sin_filtro() throws Exception {
        when(searchService.search(any())).thenReturn(List.of());

        mockMvc.perform(get(URL).param("enabled", "false")).andExpect(status().isOk());

        assertThat(criteriosRecibidos().getEnabled()).isFalse();
    }

    @Test
    void un_nombre_en_blanco_se_trata_como_sin_filtro() throws Exception {
        when(searchService.search(any())).thenReturn(List.of());

        mockMvc.perform(get(URL).param("name", "   ")).andExpect(status().isOk());

        assertThat(criteriosRecibidos().getName()).isNull();
    }

    // ---------- solicitudes invalidas ----------

    @Test
    void una_categoria_no_positiva_responde_400_con_problem_json_y_codigo_estable() throws Exception {
        mockMvc.perform(get(URL).param("categoryId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").exists());

        verifyNoInteractions(searchService);
    }

    @Test
    void un_nombre_de_mas_de_100_caracteres_responde_400() throws Exception {
        mockMvc.perform(get(URL).param("name", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(searchService);
    }

    @Test
    void una_fecha_mal_escrita_responde_400_sin_llamar_a_la_fachada() throws Exception {
        mockMvc.perform(get(URL).param("availableOn", "hola"))
                .andExpect(status().isBadRequest());

        verify(searchService, never()).search(any());
    }

    @Test
    void una_fecha_con_otro_formato_responde_400() throws Exception {
        mockMvc.perform(get(URL).param("availableOn", "20/11/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void un_booleano_mal_escrito_responde_400() throws Exception {
        mockMvc.perform(get(URL).param("enabled", "quizas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void una_categoria_no_numerica_responde_400() throws Exception {
        mockMvc.perform(get(URL).param("categoryId", "abc"))
                .andExpect(status().isBadRequest());
    }

    // ---------- fallos del servidor ----------

    @Test
    void un_dato_corrupto_de_la_base_no_se_presenta_como_un_error_del_cliente() {
        // InvalidCatalogDataException tambien la lanza el modelo ante una fila invalida: eso es un fallo del servidor
        when(searchService.search(any())).thenThrow(new InvalidCatalogDataException("fila corrupta"));

        assertThatThrownBy(() -> mockMvc.perform(get(URL)))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(InvalidCatalogDataException.class);
    }

    // ---------- metodo y ruta ----------

    @Test
    void solo_se_acepta_get() throws Exception {
        mockMvc.perform(post(URL)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void una_ruta_inexistente_responde_404() throws Exception {
        mockMvc.perform(get("/api/professionals/otra")).andExpect(status().isNotFound());
    }
}
