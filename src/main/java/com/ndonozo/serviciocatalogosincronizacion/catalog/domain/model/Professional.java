package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
public class Professional {

    private final Long id;
    private final Long categoryId;
    private final String firstName;
    private final String lastName;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    private Professional(Long id, Long categoryId, String firstName, String lastName,
                         boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = required(id, "id");
        this.categoryId = required(categoryId, "categoryId");
        this.firstName = notBlank(firstName, "firstName");
        this.lastName = notBlank(lastName, "lastName");
        this.enabled = enabled;
        this.createdAt = required(createdAt, "createdAt");
        this.updatedAt = required(updatedAt, "updatedAt");
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidCatalogDataException("Professional " + field + " must not be null");
        }
        return value;
    }

    private static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidCatalogDataException("Professional " + field + " must not be null or blank");
        }
        return value;
    }
}
