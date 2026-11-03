package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging;

import com.ndonozo.serviciocatalogosincronizacion.catalog.application.service.CatalogSyncService;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.mapper.CatalogUpdatedMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Usa el JsonMapper y el mapper reales (son baratos y así se prueba el recorrido completo
 * JSON -> dominio); solo la fachada es un mock.
 */
@ExtendWith(MockitoExtension.class)
class CatalogUpdatedListenerTest {

    private static final String EVENTO_VALIDO = """
            {
              "eventId": "catalog-event-7-4f3b",
              "eventType": "CatalogUpdated",
              "newVersion": 7,
              "occurredAt": "2026-09-10T13:44:59Z",
              "source": "MANUAL",
              "schemaVersion": 1
            }
            """;

    @Mock
    private CatalogSyncService syncService;

    private CatalogUpdatedListener listener;

    @BeforeEach
    void setUp() {
        listener = new CatalogUpdatedListener(JsonMapper.builder().build(), new CatalogUpdatedMapper(), syncService);
    }

    @Test
    void un_mensaje_valido_se_traduce_y_se_entrega_a_la_fachada() {
        listener.onMessage(EVENTO_VALIDO);

        ArgumentCaptor<CatalogUpdate> captor = ArgumentCaptor.forClass(CatalogUpdate.class);
        verify(syncService).applyCatalogUpdate(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("catalog-event-7-4f3b");
        assertThat(captor.getValue().getNewVersion()).isEqualTo(7);
    }

    @Test
    void un_campo_desconocido_no_impide_procesar_el_mensaje() {
        String conCampoNuevo = EVENTO_VALIDO.replace("\"schemaVersion\": 1", "\"schemaVersion\": 1, \"nuevo\": true");

        listener.onMessage(conCampoNuevo);

        verify(syncService).applyCatalogUpdate(any(CatalogUpdate.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "no es json", "{", "[]", "null"})
    void un_payload_ilegible_se_descarta_sin_llamar_a_la_fachada_ni_lanzar(String payload) {
        assertThatCode(() -> listener.onMessage(payload)).doesNotThrowAnyException();

        verifyNoInteractions(syncService);
    }

    @Test
    void un_numero_de_version_faltante_se_descarta() {
        String sinVersion = EVENTO_VALIDO.replace("\"newVersion\": 7,", "");

        assertThatCode(() -> listener.onMessage(sinVersion)).doesNotThrowAnyException();

        verifyNoInteractions(syncService);
    }

    @Test
    void un_event_type_distinto_se_descarta() {
        String otroTipo = EVENTO_VALIDO.replace("CatalogUpdated", "OtraCosa");

        assertThatCode(() -> listener.onMessage(otroTipo)).doesNotThrowAnyException();

        verifyNoInteractions(syncService);
    }

    @Test
    void un_schema_version_no_soportado_se_descarta() {
        String otroSchema = EVENTO_VALIDO.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2");

        assertThatCode(() -> listener.onMessage(otroSchema)).doesNotThrowAnyException();

        verifyNoInteractions(syncService);
    }

    @Test
    void un_event_id_vacio_se_descarta() {
        String sinId = EVENTO_VALIDO.replace("catalog-event-7-4f3b", " ");

        assertThatCode(() -> listener.onMessage(sinId)).doesNotThrowAnyException();

        verifyNoInteractions(syncService);
    }

    @Test
    void una_version_no_positiva_se_descarta() {
        String versionCero = EVENTO_VALIDO.replace("\"newVersion\": 7", "\"newVersion\": 0");

        assertThatCode(() -> listener.onMessage(versionCero)).doesNotThrowAnyException();

        verifyNoInteractions(syncService);
    }

    @Test
    void si_la_fachada_falla_la_excepcion_se_propaga_para_que_kafka_reintente() {
        CatedraUnavailableException error = new CatedraUnavailableException("cátedra caída", 503);
        doThrow(error).when(syncService).applyCatalogUpdate(any(CatalogUpdate.class));

        assertThatThrownBy(() -> listener.onMessage(EVENTO_VALIDO)).isSameAs(error);
    }

    @Test
    void escucha_el_topic_configurado_en_catedra_catalog_topic() throws NoSuchMethodException {
        KafkaListener anotacion = CatalogUpdatedListener.class
                .getMethod("onMessage", String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(anotacion).isNotNull();
        assertThat(anotacion.topics()).containsExactly("${catedra.catalog-topic}");
    }
}
