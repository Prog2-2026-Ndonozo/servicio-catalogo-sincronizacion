package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out;

public interface ProcessedEventPort {

    boolean isProcessed(String eventId);

    void markProcessed(String eventId, long newVersion);
}
