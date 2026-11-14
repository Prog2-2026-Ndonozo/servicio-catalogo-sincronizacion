package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.controller;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.exception.ProfessionalNotFoundException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.ProfessionalAgendaService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalAgenda;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.security.ProblemJsonSecurityHandlers;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.exception.ApiExceptionHandler;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalAgendaDtoMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.mapper.ProfessionalDtoMapper;
import com.ndonozo.serviciocatalogosincronizacion.config.SecurityConfig;
import com.ndonozo.serviciocatalogosincronizacion.support.JwtTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfessionalAgendaController.class)
@Import({ProfessionalAgendaDtoMapper.class, ProfessionalDtoMapper.class, ApiExceptionHandler.class,
        SecurityConfig.class, ProblemJsonSecurityHandlers.class})
class ProfessionalAgendaControllerTest extends JwtTestSupport {

    private static final String URL = "/api/professionals/201/agenda";
    private static final Instant FECHA = Instant.parse("2026-09-01T03:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessionalAgendaService agendaService;

    private static ProfessionalAgenda agenda() {
        Professional p = Professional.builder().id(201L).categoryId(101L).firstName("Ana").lastName("Torres")
                .enabled(true).createdAt(FECHA).updatedAt(FECHA).build();
        WeeklySchedule lunes = WeeklySchedule.builder().id(21L).professionalId(201L).dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).slotDurationMinutes(30).enabled(true)
                .createdAt(FECHA).updatedAt(FECHA).build();
        WeeklySchedule martes = WeeklySchedule.builder().id(22L).professionalId(201L).dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(14, 30)).endTime(LocalTime.of(18, 0)).slotDurationMinutes(30).enabled(false)
                .createdAt(FECHA).updatedAt(FECHA).build();
        return new ProfessionalAgenda(p, List.of(lunes, martes));
    }

    @Test
    void responde_200_con_el_profesional_y_sus_horarios() throws Exception {
        when(agendaService.getAgenda(201)).thenReturn(agenda());

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.professional.id").value(201))
                .andExpect(jsonPath("$.professional.categoryId").value(101))
                .andExpect(jsonPath("$.professional.firstName").value("Ana"))
                .andExpect(jsonPath("$.professional.lastName").value("Torres"))
                .andExpect(jsonPath("$.professional.enabled").value(true))
                .andExpect(jsonPath("$.weeklySchedules.length()").value(2))
                .andExpect(jsonPath("$.weeklySchedules[0].id").value(21))
                .andExpect(jsonPath("$.weeklySchedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.weeklySchedules[0].slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.weeklySchedules[0].enabled").value(true));
    }

    @Test
    void las_horas_se_serializan_como_texto_hh_mm_ss_y_no_como_numeros() throws Exception {
        when(agendaService.getAgenda(201)).thenReturn(agenda());

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(jsonPath("$.weeklySchedules[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[1].startTime").value("14:30:00"));
    }

    @Test
    void los_horarios_deshabilitados_se_incluyen_con_enabled_false() throws Exception {
        when(agendaService.getAgenda(201)).thenReturn(agenda());

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(jsonPath("$.weeklySchedules[1].id").value(22))
                .andExpect(jsonPath("$.weeklySchedules[1].enabled").value(false));
    }

    @Test
    void no_expone_las_fechas_de_creacion_ni_de_actualizacion() throws Exception {
        when(agendaService.getAgenda(201)).thenReturn(agenda());

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(jsonPath("$.professional.createdAt").doesNotExist())
                .andExpect(jsonPath("$.weeklySchedules[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.weeklySchedules[0].updatedAt").doesNotExist());
    }

    @Test
    void un_profesional_sin_horarios_responde_200_con_la_lista_vacia() throws Exception {
        Professional p = Professional.builder().id(201L).categoryId(101L).firstName("Ana").lastName("Torres")
                .enabled(true).createdAt(FECHA).updatedAt(FECHA).build();
        when(agendaService.getAgenda(201)).thenReturn(new ProfessionalAgenda(p, List.of()));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklySchedules").isEmpty());
    }

    @Test
    void un_profesional_inexistente_responde_404_problem_json_con_codigo_estable() throws Exception {
        when(agendaService.getAgenda(201)).thenThrow(new ProfessionalNotFoundException(201));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No existe un profesional con id 201"));
    }

    @Test
    void un_id_que_no_es_numerico_responde_400_sin_llamar_a_la_fachada() throws Exception {
        mockMvc.perform(get("/api/professionals/abc/agenda").header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(agendaService);
    }

    @Test
    void sin_token_responde_401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(agendaService);
    }

    @Test
    void con_un_token_sin_el_permiso_responde_403() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token().permiso("otro:permiso").build()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(agendaService);
    }

    @Test
    void solo_se_acepta_get() throws Exception {
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, tokenValido()))
                .andExpect(status().isMethodNotAllowed());
    }
}
