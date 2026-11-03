package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogUpdatedMessage(
        String eventId,
        String eventType,
        long newVersion,
        Instant occurredAt,
        String source,
        int schemaVersion
) {
}

// JsonIgnoreProperties con true es para la regla del contrato que declara "Los consumidores deben ignorar campos desconocidos"
// Si la catedra agregara un campo opcional, el listener no se rompe

// OcurredAt y soource no los usamos en el dominio, pero los dejamos en el DTO porque son parte del contrato y sirven para logear