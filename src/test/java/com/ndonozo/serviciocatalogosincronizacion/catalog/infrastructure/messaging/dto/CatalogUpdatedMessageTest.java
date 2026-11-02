package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que el JSON de I §15.3 se lee bien. Usa un JsonMapper sin la configuración de Spring Boot,
 * para que ignoreUnknown dependa solo de la anotación del DTO.
 */
class CatalogUpdatedMessageTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String EVENTO_DEL_CONTRATO = """
            {
              "eventId": "catalog-event-7-4f3b",
              "eventType": "CatalogUpdated",
              "newVersion": 7,
              "occurredAt": "2026-09-10T13:44:59Z",
              "source": "MANUAL",
              "schemaVersion": 1
            }
            """;

    @Test
    void lee_todos_los_campos_del_evento_del_contrato() {
        CatalogUpdatedMessage message = jsonMapper.readValue(EVENTO_DEL_CONTRATO, CatalogUpdatedMessage.class);

        assertThat(message.eventId()).isEqualTo("catalog-event-7-4f3b");
        assertThat(message.eventType()).isEqualTo("CatalogUpdated");
        assertThat(message.newVersion()).isEqualTo(7L);
        assertThat(message.occurredAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));
        assertThat(message.source()).isEqualTo("MANUAL");
        assertThat(message.schemaVersion()).isEqualTo(1);
    }

    @Test
    void ignora_campos_desconocidos_agregados_por_la_catedra() {
        String conCampoNuevo = EVENTO_DEL_CONTRATO.replace(
                "\"schemaVersion\": 1", "\"schemaVersion\": 1, \"campoNuevo\": \"opcional\"");

        CatalogUpdatedMessage message = jsonMapper.readValue(conCampoNuevo, CatalogUpdatedMessage.class);

        assertThat(message.eventId()).isEqualTo("catalog-event-7-4f3b");
        assertThat(message.newVersion()).isEqualTo(7L);
    }

    @Test
    void un_texto_ausente_queda_en_nulo_para_que_lo_rechace_el_mapper() {
        String sinEventId = EVENTO_DEL_CONTRATO.replace("\"eventId\": \"catalog-event-7-4f3b\",", "");

        CatalogUpdatedMessage message = jsonMapper.readValue(sinEventId, CatalogUpdatedMessage.class);

        assertThat(message.eventId()).isNull();
    }

    @Test
    void un_numero_ausente_hace_fallar_la_lectura_del_json() {
        String sinNewVersion = EVENTO_DEL_CONTRATO.replace("\"newVersion\": 7,", "");

        assertThatThrownBy(() -> jsonMapper.readValue(sinNewVersion, CatalogUpdatedMessage.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void un_json_ilegible_hace_fallar_la_lectura() {
        assertThatThrownBy(() -> jsonMapper.readValue("esto no es json", CatalogUpdatedMessage.class))
                .isInstanceOf(JacksonException.class);
    }
}
