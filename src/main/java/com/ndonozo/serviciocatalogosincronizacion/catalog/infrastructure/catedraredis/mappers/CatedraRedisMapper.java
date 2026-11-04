package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.mappers;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogChanges;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.VersionWindow;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dtos.CatalogChangesRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dtos.ProfessionalRedisDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CatedraRedisMapper {

    public Professional toDomain(ProfessionalRedisDto dto) {
        return Professional.builder()
                .id(dto.id())
                .categoryId(dto.categoryId())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .enabled(dto.enabled())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .build();
    }

    public CatalogChanges toDomain(CatalogChangesRedisDto dto) {
        if (dto.changes() == null) {
            throw new InvalidCatalogDataException("changes must not be null for version " + dto.version());
        }
        return CatalogChanges.builder()
                .version(dto.version())
                .categoryIds(dto.changes().professionalCategories())
                .professionalIds(dto.changes().professional())
                .weeklySchedulesIds(dto.changes().weeklySchedules())
                .build();
    }

    public VersionWindow toVersionWindow(Map<String, String> metadata) {
        return VersionWindow.builder()
                .currentVersion(parseVersion(metadata, "currentVersion"))
                .oldestAvailableVersion(parseVersion(metadata, "oldestAvailableVersion"))
                .build();
    }

    private static long parseVersion(Map<String, String> metadata, String field) {
        String value = metadata.get(field);
        if (value == null) {
            throw new InvalidCatalogDataException("Redis metadata is missing " + field);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidCatalogDataException("Redis metadata " + field + " is not a number: " + value);
        }
    }
}
