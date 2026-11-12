package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler  {

    @ExceptionHandler(InvalidSearchRequestException.class)
    public ProblemDetail handle(InvalidSearchRequestException e){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

}
