package com.ndonozo.serviciocatalogosincronizacion.catalog.application.exception;

public class ProfessionalNotFoundException extends RuntimeException {

    private final long professionalId;

    public ProfessionalNotFoundException(long professionalId) {
        super("Could not find professional with id: " + professionalId);
        this.professionalId = professionalId;
    }

    public long getProfessionalId() {
        return professionalId;
    }
}
