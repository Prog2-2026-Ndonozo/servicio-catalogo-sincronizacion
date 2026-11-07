package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception;

public class FullSyncRequiredException extends RuntimeException {
    public FullSyncRequiredException(String message) {
        super(message);
    }
}
