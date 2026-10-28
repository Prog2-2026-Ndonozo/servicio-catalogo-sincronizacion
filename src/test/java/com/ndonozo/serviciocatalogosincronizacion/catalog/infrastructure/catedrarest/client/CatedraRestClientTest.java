package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.client;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CatedraSnapshotResponse;
import com.ndonozo.serviciocatalogosincronizacion.config.CatedraProperties;
import com.ndonozo.serviciocatalogosincronizacion.config.RestClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatedraRestClientTest {

    private static final String BASE_URL = "http://catedra.test";
    private static final String JWT = "eyJ.secreto.jwt";
    private static final String SNAPSHOT_URL = BASE_URL + "/api/synchronization/snapshot";

    private static final String SNAPSHOT_JSON = """
            {
              "snapshotVersion": 7,
              "generatedAt": "2026-09-10T13:44:59Z",
              "professionalCategories": [
                {"id": 1, "name": "Clinica medica", "description": "Atencion clinica general",
                 "enabled": true, "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
              ],
              "professionals": [
                {"id": 15, "categoryId": 1, "firstName": "Ana", "lastName": "Gomez",
                 "enabled": true, "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
              ],
              "weeklySchedules": [
                {"id": 21, "professionalId": 15, "dayOfWeek": "MONDAY", "startTime": "09:00:00",
                 "endTime": "12:00", "slotDurationMinutes": 30,
                 "enabled": true, "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
              ]
            }
            """;

    private MockRestServiceServer server;
    private CatedraRestClient client;

    @BeforeEach
    void preparaCliente() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = new RestClientConfig()
                .catedraRestClient(builder, new CatedraProperties(BASE_URL, JWT));
        client = new CatedraRestClient(restClient);
    }

    @Test
    void con_respuesta_valida_devuelve_el_snapshot_leido() {
        server.expect(requestTo(SNAPSHOT_URL))
                .andExpect(method(GET))
                .andRespond(withSuccess(SNAPSHOT_JSON, APPLICATION_JSON));

        CatedraSnapshotResponse response = client.fetchSnapshot();

        assertThat(response.snapshotVersion()).isEqualTo(7L);
        assertThat(response.professionalCategories()).hasSize(1);
        assertThat(response.professionalCategories().getFirst().name()).isEqualTo("Clinica medica");
        assertThat(response.professionals()).hasSize(1);
        assertThat(response.professionals().getFirst().categoryId()).isEqualTo(1L);
        assertThat(response.weeklySchedules()).hasSize(1);
        assertThat(response.weeklySchedules().getFirst().dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.weeklySchedules().getFirst().startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.weeklySchedules().getFirst().endTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(response.weeklySchedules().getFirst().createdAt())
                .isEqualTo(Instant.parse("2026-07-01T12:00:00Z"));
        server.verify();
    }

    @Test
    void envia_el_jwt_en_el_header_authorization() {
        server.expect(requestTo(SNAPSHOT_URL))
                .andExpect(header(AUTHORIZATION, "Bearer " + JWT))
                .andRespond(withSuccess(SNAPSHOT_JSON, APPLICATION_JSON));

        client.fetchSnapshot();

        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500, 503})
    void un_status_de_error_se_traduce_a_catedra_no_disponible_con_ese_status(int status) {
        server.expect(requestTo(SNAPSHOT_URL))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)));

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOfSatisfying(CatedraUnavailableException.class,
                        e -> assertThat(e.getHttpStatus()).isEqualTo(status));
    }

    @Test
    void el_mensaje_de_error_no_expone_el_jwt() {
        server.expect(requestTo(SNAPSHOT_URL))
                .andRespond(withStatus(HttpStatusCode.valueOf(401)));

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOf(CatedraUnavailableException.class)
                .hasMessageNotContaining(JWT);
    }

    @Test
    void un_cuerpo_vacio_se_traduce_a_catedra_no_disponible_sin_status() {
        server.expect(requestTo(SNAPSHOT_URL))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOfSatisfying(CatedraUnavailableException.class,
                        e -> assertThat(e.getHttpStatus()).isNull());
    }

    @Test
    void un_json_ilegible_se_traduce_a_catedra_no_disponible_conservando_la_causa() {
        server.expect(requestTo(SNAPSHOT_URL))
                .andRespond(withSuccess("esto no es json", APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOfSatisfying(CatedraUnavailableException.class, e -> {
                    assertThat(e.getHttpStatus()).isNull();
                    assertThat(e.getCause()).isNotNull();
                });
    }

    @Test
    void un_error_de_red_se_traduce_a_catedra_no_disponible_conservando_la_causa() {
        server.expect(requestTo(SNAPSHOT_URL))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOfSatisfying(CatedraUnavailableException.class, e -> {
                    assertThat(e.getHttpStatus()).isNull();
                    assertThat(e.getCause()).isNotNull();
                });
    }
}
