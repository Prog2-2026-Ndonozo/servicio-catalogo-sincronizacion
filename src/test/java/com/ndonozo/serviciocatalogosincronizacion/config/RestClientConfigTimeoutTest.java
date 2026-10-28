package com.ndonozo.serviciocatalogosincronizacion.config;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.client.CatedraRestClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RestClientConfigTimeoutTest {

    private static final long RESPUESTA_LENTA_MS = 3_000;

    private HttpServer servidorLento;

    @BeforeEach
    void levantaServidorQueTardaEnResponder() throws IOException {
        servidorLento = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidorLento.createContext("/", exchange -> {
            try {
                Thread.sleep(RESPUESTA_LENTA_MS);
                byte[] cuerpo = "{}".getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, cuerpo.length);
                exchange.getResponseBody().write(cuerpo);
            } catch (InterruptedException | IOException ignorada) {
                // el cliente cortó antes de recibir la respuesta
            } finally {
                exchange.close();
            }
        });
        servidorLento.start();
    }

    @AfterEach
    void apagaServidor() {
        servidorLento.stop(0);
    }

    private ApplicationContextRunner contexto(String... propiedadesExtra) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
                .withUserConfiguration(RestClientConfig.class)
                .withPropertyValues(
                        "catedra.base-url=http://localhost:" + servidorLento.getAddress().getPort(),
                        "catedra.jwt=jwt-de-prueba")
                .withPropertyValues(propiedadesExtra);
    }

    @Test
    void el_read_timeout_configurado_corta_una_respuesta_lenta() {
        contexto("spring.http.clients.read-timeout=300ms").run(ctx -> {
            CatedraRestClient cliente = new CatedraRestClient(ctx.getBean(RestClient.class));

            long inicio = System.nanoTime();
            Throwable error = catchThrowable(cliente::fetchSnapshot);
            Duration espera = Duration.ofNanos(System.nanoTime() - inicio);

            assertThat(error).isInstanceOf(CatedraUnavailableException.class);
            assertThat(espera).isLessThan(Duration.ofMillis(RESPUESTA_LENTA_MS));
        });
    }

    @Test
    void sin_read_timeout_la_misma_respuesta_lenta_se_espera_completa() {
        contexto().run(ctx -> {
            CatedraRestClient cliente = new CatedraRestClient(ctx.getBean(RestClient.class));

            long inicio = System.nanoTime();
            catchThrowable(cliente::fetchSnapshot);
            Duration espera = Duration.ofNanos(System.nanoTime() - inicio);

            assertThat(espera).isGreaterThanOrEqualTo(Duration.ofMillis(RESPUESTA_LENTA_MS - 200));
        });
    }
}
