package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.exception;

public class InvalidSearchRequestException extends RuntimeException {
    public InvalidSearchRequestException(String message) {
        super(message);
    }
}
