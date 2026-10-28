package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception;

import lombok.Getter;

@Getter
public class CatedraUnavailableException extends RuntimeException {
    private final Integer httpStatus;

    public CatedraUnavailableException(String message){
        super(message);
        this.httpStatus= null;

    }
    public CatedraUnavailableException(String message, int httpstatus){
        super(message);
        this.httpStatus = httpstatus ;

    }
    public CatedraUnavailableException(String message, Throwable cause){
        super(message, cause);
        this.httpStatus = null;
    }

}
