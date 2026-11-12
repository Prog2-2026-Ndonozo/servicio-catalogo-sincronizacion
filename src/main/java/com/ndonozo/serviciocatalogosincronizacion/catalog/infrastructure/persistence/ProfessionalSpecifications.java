package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Expression;

import java.time.DayOfWeek;
import java.util.Locale;

public final class ProfessionalSpecifications {

    private static final char ESCAPE = '\\';

    private ProfessionalSpecifications() {
    }

    public static Specification<ProfessionalEntity> from(ProfessionalSearchCriteria criteria) {
        Specification<ProfessionalEntity> spec = Specification.unrestricted();
        if (criteria.getCategoryId() != null) {
            spec = spec.and(inCategory(criteria.getCategoryId()));
        }
        if (criteria.getName() != null) {
            spec = spec.and(nameContains(criteria.getName()));
        }
        if (criteria.getEnabled() != null) {
            spec = spec.and(hasEnabled(criteria.getEnabled()));
        }
        if (criteria.getAvailableOn() != null) {
            spec = spec.and(worksOn(criteria.getAvailableOn().getDayOfWeek()));
        }
        return spec;
    }

    private static Specification<ProfessionalEntity> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    private static Specification<ProfessionalEntity> hasEnabled(boolean enabled) {
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    private static Specification<ProfessionalEntity> nameContains(String name) {
        String pattern = "%" + escapeLike(name.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Expression<String> fullName = cb.lower(
                    cb.concat(cb.concat(root.<String>get("firstName"), " "), root.<String>get("lastName")));
            return cb.like(fullName, pattern, ESCAPE);
        };
    }

    private static Specification<ProfessionalEntity> worksOn(DayOfWeek day) {
        return (root, query, cb) -> {
            Subquery<Long> schedules = query.subquery(Long.class);
            Root<WeeklyScheduleEntity> schedule = schedules.from(WeeklyScheduleEntity.class);
            schedules.select(schedule.<Long>get("id")).where(
                    cb.equal(schedule.get("professionalId"), root.get("id")),
                    cb.isTrue(schedule.<Boolean>get("enabled")),
                    cb.equal(schedule.get("dayOfWeek"), day));
            return cb.exists(schedules);
        };
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
