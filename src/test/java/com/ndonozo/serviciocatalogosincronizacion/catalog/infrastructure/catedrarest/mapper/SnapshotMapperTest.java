package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogSnapshot;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CatedraSnapshotResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CategoryDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.ProfessionalDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.WeeklyScheduleDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotMapperTest {

    private static final Instant CATEGORIA_CREADA = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant CATEGORIA_ACTUALIZADA = Instant.parse("2026-07-02T13:00:00Z");
    private static final Instant PROFESIONAL_CREADO = Instant.parse("2026-07-03T14:00:00Z");
    private static final Instant PROFESIONAL_ACTUALIZADO = Instant.parse("2026-07-04T15:00:00Z");
    private static final Instant HORARIO_CREADO = Instant.parse("2026-07-05T16:00:00Z");
    private static final Instant HORARIO_ACTUALIZADO = Instant.parse("2026-07-06T17:00:00Z");

    private final SnapshotMapper mapper = new SnapshotMapper();

    private static CategoryDto categoria(long id, String description) {
        return new CategoryDto(id, "Clinica medica", description, true, CATEGORIA_CREADA, CATEGORIA_ACTUALIZADA);
    }

    private static ProfessionalDto profesional(long id) {
        return new ProfessionalDto(id, 3L, "Ana", "Gomez", false, PROFESIONAL_CREADO, PROFESIONAL_ACTUALIZADO);
    }

    private static WeeklyScheduleDto horario(long id) {
        return new WeeklyScheduleDto(id, 15L, WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true,
                HORARIO_CREADO, HORARIO_ACTUALIZADO);
    }

    private static CatedraSnapshotResponse respuesta(List<CategoryDto> categorias,
                                                     List<ProfessionalDto> profesionales,
                                                     List<WeeklyScheduleDto> horarios) {
        return new CatedraSnapshotResponse(7L, categorias, profesionales, horarios);
    }

    @Test
    void traduce_el_snapshot_completo_sin_cruzar_campos() {
        CatedraSnapshotResponse response = respuesta(
                List.of(categoria(1L, "Atencion clinica general")),
                List.of(profesional(15L)),
                List.of(horario(21L)));

        CatalogSnapshot snapshot = mapper.toDomain(response);

        assertThat(snapshot.getSnapshotVersion()).isEqualTo(7L);

        ProfessionalCategory categoria = snapshot.getCategories().getFirst();
        assertThat(categoria.getId()).isEqualTo(1L);
        assertThat(categoria.getName()).isEqualTo("Clinica medica");
        assertThat(categoria.getDescription()).isEqualTo("Atencion clinica general");
        assertThat(categoria.isEnabled()).isTrue();
        assertThat(categoria.getCreatedAt()).isEqualTo(CATEGORIA_CREADA);
        assertThat(categoria.getUpdatedAt()).isEqualTo(CATEGORIA_ACTUALIZADA);

        Professional profesional = snapshot.getProfessionals().getFirst();
        assertThat(profesional.getId()).isEqualTo(15L);
        assertThat(profesional.getCategoryId()).isEqualTo(3L);
        assertThat(profesional.getFirstName()).isEqualTo("Ana");
        assertThat(profesional.getLastName()).isEqualTo("Gomez");
        assertThat(profesional.isEnabled()).isFalse();
        assertThat(profesional.getCreatedAt()).isEqualTo(PROFESIONAL_CREADO);
        assertThat(profesional.getUpdatedAt()).isEqualTo(PROFESIONAL_ACTUALIZADO);

        WeeklySchedule horario = snapshot.getWeeklySchedules().getFirst();
        assertThat(horario.getId()).isEqualTo(21L);
        assertThat(horario.getProfessionalId()).isEqualTo(15L);
        assertThat(horario.getDayOfWeek()).isEqualTo(WEDNESDAY);
        assertThat(horario.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(horario.getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(horario.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(horario.isEnabled()).isTrue();
        assertThat(horario.getCreatedAt()).isEqualTo(HORARIO_CREADO);
        assertThat(horario.getUpdatedAt()).isEqualTo(HORARIO_ACTUALIZADO);


        // Busca detectar campos cruzados. Que cada fecha sea distinta entre categorias , professionales y horario
        // si intercambiamos los startTime/endTime haria fallar el test
    }

    @Test
    void la_descripcion_nula_se_traduce_como_cadena_vacia() {
        CatedraSnapshotResponse response = respuesta(List.of(categoria(1L, null)), List.of(), List.of());

        CatalogSnapshot snapshot = mapper.toDomain(response);

        assertThat(snapshot.getCategories().getFirst().getDescription()).isEmpty();

        // Detecta la regla propia del mapper
    }

    @Test
    void la_descripcion_con_texto_se_conserva() {
        CatedraSnapshotResponse response = respuesta(List.of(categoria(1L, "Detalle")), List.of(), List.of());

        CatalogSnapshot snapshot = mapper.toDomain(response);

        assertThat(snapshot.getCategories().getFirst().getDescription()).isEqualTo("Detalle");

        // Que la regla no pise descripciones reales
    }

    @Test
    void las_listas_vacias_dan_un_snapshot_valido() {
        CatalogSnapshot snapshot = mapper.toDomain(respuesta(List.of(), List.of(), List.of()));

        assertThat(snapshot.getSnapshotVersion()).isEqualTo(7L);
        assertThat(snapshot.getCategories()).isEmpty();
        assertThat(snapshot.getProfessionals()).isEmpty();
        assertThat(snapshot.getWeeklySchedules()).isEmpty();

        //  detecta  el caso  borde de listas vaicas
    }

    @Test
    void con_varios_elementos_conserva_cantidad_y_orden() {
        CatedraSnapshotResponse response = respuesta(
                List.of(categoria(3L, "a"), categoria(1L, "b"), categoria(2L, "c")),
                List.of(profesional(30L), profesional(10L)),
                List.of(horario(5L), horario(4L)));

        CatalogSnapshot snapshot = mapper.toDomain(response);

        assertThat(snapshot.getCategories()).extracting(ProfessionalCategory::getId).containsExactly(3L, 1L, 2L);
        assertThat(snapshot.getProfessionals()).extracting(Professional::getId).containsExactly(30L, 10L);
        assertThat(snapshot.getWeeklySchedules()).extracting(WeeklySchedule::getId).containsExactly(5L, 4L);

        // detectamos que no se pierdan ni se reodernen elementos
    }

    @ParameterizedTest
    @NullAndEmptySource
    void un_dto_invalido_no_se_traduce_y_propaga_el_error_del_dominio(String nombreInvalido) {
        CategoryDto invalida = new CategoryDto(1L, nombreInvalido, "x", true, CATEGORIA_CREADA, CATEGORIA_ACTUALIZADA);
        CatedraSnapshotResponse response = respuesta(List.of(invalida), List.of(), List.of());

        assertThatThrownBy(() -> mapper.toDomain(response))
                .isInstanceOf(InvalidCatalogDataException.class);


    } // detecta que un dto invalido se lanza la excepcion, que es de lo que depende que falle la transaccion
}
