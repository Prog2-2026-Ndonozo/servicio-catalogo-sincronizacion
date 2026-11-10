package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.*;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatedraCatalogReadPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.CatalogChangesRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.CategoryRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.ProfessionalRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.WeeklyScheduleRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.mapper.CatedraRedisMapper;
import lombok.RequiredArgsConstructor;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class CatedraRedisAdapter implements CatedraCatalogReadPort {

    private static final String METADATA_KEY = "catedra:sync:metadata";
    private static final String CHANGES_KEY_PREFIX = "catedra:sync:changes:";
    private static final String CATEGORIES_KEY = "catedra:sync:professional-categories";
    private static final String PROFESSIONALS_KEY = "catedra:sync:professionals";
    private static final String SCHEDULES_KEY = "catedra:sync:weekly-schedules";

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final CatedraRedisMapper mapper;

    @Override
    public VersionWindow fetchVersionWindow() {
        Map<Object, Object> entries = readRedis(() -> redis.opsForHash().entries(METADATA_KEY));
        Map<String, String> metadata = new HashMap<>();
        entries.forEach((field, value) -> metadata.put(String.valueOf(field), String.valueOf(value)));
        return mapper.toVersionWindow(metadata);
    }

    @Override
    public Optional<CatalogChanges> fetchChanges(long version) {
        String key = CHANGES_KEY_PREFIX + version;
        String json = readRedis(() -> redis.opsForValue().get(key));
        if (json == null) {
            return Optional.empty();
        }
        CatalogChanges changes = mapper.toDomain(parse(json, CatalogChangesRedisDto.class, key));
        if (changes.getVersion() != version) {
            throw new InvalidCatalogDataException(
                    "Redis changes for version " + version + " declare version " + changes.getVersion());
        }
        return Optional.of(changes);
    }

    @Override
    public List<ProfessionalCategory> fetchCategories(Collection<Long> ids) {
        return fetchEntities(CATEGORIES_KEY, ids, CategoryRedisDto.class, mapper::toDomain);
    }

    @Override
    public List<Professional> fetchProfessional(Collection<Long> ids) {
        return fetchEntities(PROFESSIONALS_KEY, ids, ProfessionalRedisDto.class, mapper::toDomain);
    }

    @Override
    public List<WeeklySchedule> fetchWeeklySchedule(Collection<Long> ids) {
        return fetchEntities(SCHEDULES_KEY, ids, WeeklyScheduleRedisDto.class, mapper::toDomain);
    }

    private <D, T> List<T> fetchEntities(String hashKey, Collection<Long> ids,
                                         Class<D> dtoType, Function<D, T> toDomain) {
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(ids));
        if (uniqueIds.isEmpty()) {
            return List.of();
        }
        List<Object> fields = new ArrayList<>();
        uniqueIds.forEach(id -> fields.add(String.valueOf(id)));
        List<Object> values = readRedis(() -> redis.opsForHash().multiGet(hashKey, fields));

        List<T> entities = new ArrayList<>();
        List<Long> missing = new ArrayList<>();
        for (int i = 0; i < uniqueIds.size(); i++) {
            Object value = values.get(i);
            if (value == null) {
                missing.add(uniqueIds.get(i));
            } else {
                entities.add(toDomain.apply(parse(value.toString(), dtoType, hashKey + "/" + uniqueIds.get(i))));
            }
        }
        if (!missing.isEmpty()) {
            throw new InvalidCatalogDataException("Redis " + hashKey + " has no entries for ids " + missing);
        }
        return entities;
    }

    private <T> T readRedis(Supplier<T> command) {
        try {
            return command.get();
        } catch (DataAccessException e) {
            throw new CatedraUnavailableException("Redis de la catedra no disponible", e);
        }
    }

    private <D> D parse(String json, Class<D> type, String source) {
        try {
            D dto = jsonMapper.readValue(json, type);
            if (dto == null) {
                throw new InvalidCatalogDataException("Redis value is empty at " + source);
            }
            return dto;
        } catch (JacksonException e) {
            throw new InvalidCatalogDataException("Redis value is not readable at " + source);
        }
    }
}