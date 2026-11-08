package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, e) -> log.error(
                        "CatalogUpdated descartado tras agotar los reintentos (partition={}, offset={}, key={})",
                        record.partition(), record.offset(), record.key(), e),
                backOff);

        handler.setRetryListeners((record, e, attempt) -> log.warn(
                "Fallo al aplicar CatalogUpdated (offset={}, intento={}): {}",
                record.offset(), attempt,
                e == null ? "sin detalle" : (e.getCause() != null ? e.getCause() : e).toString()));
        return handler;
    }
}

// El backoff no garantiza perder un evento, el evento siguiente repara ese hueco. Pero eso depende de que llegue otro evento
// El evento siguiente repara el hueco