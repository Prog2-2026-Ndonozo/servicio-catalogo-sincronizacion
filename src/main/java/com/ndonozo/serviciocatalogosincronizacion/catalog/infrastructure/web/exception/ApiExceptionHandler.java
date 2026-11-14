package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.web.exception;


import com.ndonozo.serviciocatalogosincronizacion.catalog.application.exception.ProfessionalNotFoundException;
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

    @ExceptionHandler(ProfessionalNotFoundException.class)
    public ProblemDetail handle(ProfessionalNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "No existe un profesional con id " + e.getProfessionalId());
        problem.setProperty("code", "PROFESSIONAL_NOT_FOUND");
        return problem;
    }
}
