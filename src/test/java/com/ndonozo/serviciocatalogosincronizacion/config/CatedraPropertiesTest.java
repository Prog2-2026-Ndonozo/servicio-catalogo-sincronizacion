package com.ndonozo.serviciocatalogosincronizacion.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatedraPropertiesTest {

    private static final String BASE_URL = "http://localhost:8090";
    private static final String JWT = "eyJ.secreto.jwt";

    private static CatedraProperties propiedades() {
        return new CatedraProperties(BASE_URL, JWT);
    }

    @Test
    void conserva_base_url_y_jwt() {
        CatedraProperties propiedades = propiedades();

        assertThat(propiedades.baseUrl()).isEqualTo(BASE_URL);
        assertThat(propiedades.jwt()).isEqualTo(JWT);
    }

    @Test
    void to_string_no_expone_el_jwt() {
        assertThat(propiedades().toString()).doesNotContain(JWT);
    }

    @Test
    void to_string_oculta_el_jwt_y_muestra_la_base_url() {
        assertThat(propiedades().toString())
                .contains(BASE_URL)
                .contains("jwt=****");
    }
}
