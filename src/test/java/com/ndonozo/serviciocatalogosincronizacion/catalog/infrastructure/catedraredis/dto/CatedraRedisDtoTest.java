package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que los JSON de I §14.4 y §14.5 se leen bien. Usa un JsonMapper sin la configuración de
 * Spring Boot, para que ignoreUnknown dependa solo de la anotación de cada DTO.
 */
class CatedraRedisDtoTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String PROFESIONAL = """
            {
              "id": 15,
              "categoryId": 1,
              "firstName": "Ana",
              "lastName": "Gomez",
              "enabled": true,
              "createdAt": "2026-07-01T12:00:00Z",
              "updatedAt": "2026-09-10T13:40:00Z"
            }
            """;

    private static final String CATEGORIA = """
            {
              "id": 1,
              "name": "Clinica medica",
              "description": "Atencion clinica general",
              "enabled": true,
              "createdAt": "2026-07-01T12:00:00Z",
              "updatedAt": "2026-09-10T13:40:00Z"
            }
            """;

    private static final String HORARIO = """
            {
              "id": 21,
              "professionalId": 15,
              "dayOfWeek": "MONDAY",
              "startTime": "09:00:00",
              "endTime": "12:00:00",
              "slotDurationMinutes": 30,
              "enabled": true,
              "createdAt": "2026-07-01T12:00:00Z",
              "updatedAt": "2026-09-10T13:40:00Z"
            }
            """;

    private static final String CAMBIOS = """
            {
              "version": 7,
              "changes": {
                "professionalCategories": [],
                "professionals": [15],
                "weeklySchedules": []
              }
            }
            """;

    @Test
    void lee_un_profesional_del_contrato() {
        ProfessionalRedisDto dto = jsonMapper.readValue(PROFESIONAL, ProfessionalRedisDto.class);

        assertThat(dto.id()).isEqualTo(15L);
        assertThat(dto.categoryId()).isEqualTo(1L);
        assertThat(dto.firstName()).isEqualTo("Ana");
        assertThat(dto.lastName()).isEqualTo("Gomez");
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.createdAt()).isEqualTo(Instant.parse("2026-07-01T12:00:00Z"));
        assertThat(dto.updatedAt()).isEqualTo(Instant.parse("2026-09-10T13:40:00Z"));
    }

    @Test
    void lee_una_categoria_del_contrato() {
        CategoryRedisDto dto = jsonMapper.readValue(CATEGORIA, CategoryRedisDto.class);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Clinica medica");
        assertThat(dto.description()).isEqualTo("Atencion clinica general");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    void lee_un_horario_semanal_del_contrato() {
        WeeklyScheduleRedisDto dto = jsonMapper.readValue(HORARIO, WeeklyScheduleRedisDto.class);

        assertThat(dto.id()).isEqualTo(21L);
        assertThat(dto.professionalId()).isEqualTo(15L);
        assertThat(dto.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(dto.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(dto.endTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(dto.slotDurationMinutes()).isEqualTo(30);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    void lee_los_cambios_de_una_version_del_contrato() {
        CatalogChangesRedisDto dto = jsonMapper.readValue(CAMBIOS, CatalogChangesRedisDto.class);

        assertThat(dto.version()).isEqualTo(7L);
        assertThat(dto.changes().professionalCategories()).isEmpty();
        assertThat(dto.changes().professionals()).containsExactly(15L);
        assertThat(dto.changes().weeklySchedules()).isEmpty();
    }

    @Test
    void ignora_campos_desconocidos() {
        String conCampoNuevo = PROFESIONAL.replace("\"enabled\": true,", "\"enabled\": true, \"nuevo\": \"x\",");

        ProfessionalRedisDto dto = jsonMapper.readValue(conCampoNuevo, ProfessionalRedisDto.class);

        assertThat(dto.id()).isEqualTo(15L);
    }

    @Test
    void ignora_campos_desconocidos_tambien_en_los_cambios() {
        String conCampoNuevo = CAMBIOS.replace("\"version\": 7,", "\"version\": 7, \"nuevo\": 1,")
                .replace("\"weeklySchedules\": []", "\"weeklySchedules\": [], \"otro\": [3]");

        CatalogChangesRedisDto dto = jsonMapper.readValue(conCampoNuevo, CatalogChangesRedisDto.class);

        assertThat(dto.changes().professionals()).containsExactly(15L);
    }

    @Test
    void un_campo_de_texto_faltante_queda_nulo_para_que_lo_rechace_el_dominio() {
        String sinNombre = PROFESIONAL.replace("\"firstName\": \"Ana\",", "");

        ProfessionalRedisDto dto = jsonMapper.readValue(sinNombre, ProfessionalRedisDto.class);

        assertThat(dto.firstName()).isNull();
    }

    @Test
    void un_booleano_faltante_falla_al_leer() {
        String sinEnabled = PROFESIONAL.replace("\"enabled\": true,", "");

        assertThatThrownBy(() -> jsonMapper.readValue(sinEnabled, ProfessionalRedisDto.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void un_objeto_changes_faltante_queda_nulo() {
        CatalogChangesRedisDto dto = jsonMapper.readValue("{\"version\": 7}", CatalogChangesRedisDto.class);

        assertThat(dto.changes()).isNull();
    }

    @Test
    void un_dia_de_la_semana_desconocido_falla_al_leer() {
        String diaInvalido = HORARIO.replace("MONDAY", "LUNES");

        assertThatThrownBy(() -> jsonMapper.readValue(diaInvalido, WeeklyScheduleRedisDto.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void un_json_ilegible_falla_al_leer() {
        assertThatThrownBy(() -> jsonMapper.readValue("no es json", ProfessionalRedisDto.class))
                .isInstanceOf(JacksonException.class);
    }
}
