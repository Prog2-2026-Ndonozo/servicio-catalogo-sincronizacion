package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.dto.CatalogUpdatedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogUpdatedMapperTest {

    private final CatalogUpdatedMapper mapper = new CatalogUpdatedMapper();

    private static CatalogUpdatedMessage mensaje(String eventId, String eventType, long newVersion, int schemaVersion) {
        return new CatalogUpdatedMessage(eventId, eventType, newVersion,
                Instant.parse("2026-09-10T13:44:59Z"), "MANUAL", schemaVersion);
    }

    private static CatalogUpdatedMessage mensajeValido() {
        return mensaje("catalog-event-7-4f3b", "CatalogUpdated", 7, 1);
    }

    @Test
    void un_mensaje_valido_se_traduce_al_dominio() {
        CatalogUpdate update = mapper.toDomain(mensajeValido());

        assertThat(update.getEventId()).isEqualTo("catalog-event-7-4f3b");
        assertThat(update.getNewVersion()).isEqualTo(7L);
    }

    @Test
    void occurred_at_y_source_no_influyen_en_la_traduccion() {
        CatalogUpdatedMessage sinMetadata = new CatalogUpdatedMessage(
                "catalog-event-7-4f3b", "CatalogUpdated", 7, null, null, 1);

        CatalogUpdate update = mapper.toDomain(sinMetadata);

        assertThat(update.getEventId()).isEqualTo("catalog-event-7-4f3b");
        assertThat(update.getNewVersion()).isEqualTo(7L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"CatalogUpdate", "catalogupdated", "CatalogUpdated ", "AppointmentConfirmed"})
    void rechaza_un_tipo_de_evento_distinto_de_catalog_updated(String eventTypeInvalido) {
        assertThatThrownBy(() -> mapper.toDomain(mensaje("catalog-event-7-4f3b", eventTypeInvalido, 7, 1)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("eventType");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 2, 99})
    void rechaza_una_version_de_esquema_no_soportada(int schemaVersion) {
        assertThatThrownBy(() -> mapper.toDomain(mensaje("catalog-event-7-4f3b", "CatalogUpdated", 7, schemaVersion)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("schemaVersion");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void un_event_id_invalido_lo_rechaza_el_dominio(String eventIdInvalido) {
        assertThatThrownBy(() -> mapper.toDomain(mensaje(eventIdInvalido, "CatalogUpdated", 7, 1)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("eventId");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void una_version_no_positiva_la_rechaza_el_dominio(long versionInvalida) {
        assertThatThrownBy(() -> mapper.toDomain(mensaje("catalog-event-7-4f3b", "CatalogUpdated", versionInvalida, 1)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("newVersion");
    }
}
