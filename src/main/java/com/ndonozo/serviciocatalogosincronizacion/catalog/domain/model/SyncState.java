package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import java.time.Instant;

public record SyncState(
        SyncStatus status,
        String detail,
        Instant updatedAt
) {
}
