package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que la configuracion se registra como bean (sin @Configuration, Spring Boot no la tomaria y el
 * listener seguiria con el manejo por defecto: 10 intentos sin espera) y que el manejador es el esperado.
 * Los intervalos de espera se verifican a mano mirando los WARN del log (ADR-0030).
 */
class KafkaErrorHandlingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaErrorHandlingConfig.class);

    @Test
    void la_clase_esta_anotada_como_configuracion() {
        assertThat(KafkaErrorHandlingConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
    }

    @Test
    void registra_un_manejador_de_errores_de_kafka_por_defecto_con_reintentos() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CommonErrorHandler.class);
            assertThat(context.getBean(CommonErrorHandler.class)).isInstanceOf(DefaultErrorHandler.class);
        });
    }
}
