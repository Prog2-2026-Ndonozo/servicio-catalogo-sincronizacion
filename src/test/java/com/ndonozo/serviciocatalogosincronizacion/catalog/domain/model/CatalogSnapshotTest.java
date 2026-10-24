package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogSnapshotTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    private static ProfessionalCategory categoria() {
        return ProfessionalCategory.builder()
                .id(1L)
                .name("Clinica medica")
                .description("Atencion clinica general")
                .enabled(true)
                .createdAt(CREADO)
                .updatedAt(ACTUALIZADO)
                .build();
    }

    private static Professional profesional() {
        return Professional.builder()
                .id(15L)
                .categoryId(1L)
                .firstName("Ana")
                .lastName("Gomez")
                .enabled(true)
                .createdAt(CREADO)
                .updatedAt(ACTUALIZADO)
                .build();
    }

    private static WeeklySchedule horario() {
        return WeeklySchedule.builder()
                .id(21L)
                .professionalId(15L)
                .dayOfWeek(MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .enabled(true)
                .createdAt(CREADO)
                .updatedAt(ACTUALIZADO)
                .build();
    }

    private static CatalogSnapshot.CatalogSnapshotBuilder snapshotValido() {
        return CatalogSnapshot.builder()
                .snapshotVersion(7)
                .categories(List.of(categoria()))
                .professionals(List.of(profesional()))
                .weeklySchedules(List.of(horario()));
    }

    // ---------- creacion valida ----------

    @Test
    void con_datos_validos_se_crea_con_los_valores_recibidos() {
        CatalogSnapshot snapshot = snapshotValido().build();

        assertThat(snapshot.getSnapshotVersion()).isEqualTo(7L);
        assertThat(snapshot.getCategories()).hasSize(1);
        assertThat(snapshot.getProfessionals()).hasSize(1);
        assertThat(snapshot.getWeeklySchedules()).hasSize(1);
        assertThat(snapshot.getCategories().getFirst().getId()).isEqualTo(1L);
        assertThat(snapshot.getProfessionals().getFirst().getId()).isEqualTo(15L);
        assertThat(snapshot.getWeeklySchedules().getFirst().getId()).isEqualTo(21L);
    }

    @Test
    void las_listas_vacias_son_validas() {
        CatalogSnapshot snapshot = snapshotValido()
                .categories(List.of())
                .professionals(List.of())
                .weeklySchedules(List.of())
                .build();

        assertThat(snapshot.getCategories()).isEmpty();
        assertThat(snapshot.getProfessionals()).isEmpty();
        assertThat(snapshot.getWeeklySchedules()).isEmpty();
    }

    // ---------- snapshotVersion ----------

    @Test
    void la_version_0_es_valida() {
        CatalogSnapshot snapshot = snapshotValido().snapshotVersion(0).build();

        assertThat(snapshot.getSnapshotVersion()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100, Long.MIN_VALUE})
    void rechaza_version_negativa(long versionNegativa) {
        assertThatThrownBy(() -> snapshotValido().snapshotVersion(versionNegativa).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("snapshotVersion");
    }

    // ---------- listas nulas ----------

    static Stream<Arguments> listasNulas() {
        return Stream.of(
                Arguments.of("categories",
                        (UnaryOperator<CatalogSnapshot.CatalogSnapshotBuilder>) b -> b.categories(null)),
                Arguments.of("professionals",
                        (UnaryOperator<CatalogSnapshot.CatalogSnapshotBuilder>) b -> b.professionals(null)),
                Arguments.of("weeklySchedules",
                        (UnaryOperator<CatalogSnapshot.CatalogSnapshotBuilder>) b -> b.weeklySchedules(null))
        );
    }

    @ParameterizedTest(name = "rechaza {0} nula")
    @MethodSource("listasNulas")
    void rechaza_listas_nulas(String campo, UnaryOperator<CatalogSnapshot.CatalogSnapshotBuilder> anulaCampo) {
        assertThatThrownBy(() -> anulaCampo.apply(snapshotValido()).build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining(campo);
    }

    // ---------- copia defensiva ----------

    @Test
    void guarda_una_copia_de_las_listas_recibidas() {
        List<ProfessionalCategory> categorias = new ArrayList<>(List.of(categoria()));
        List<Professional> profesionales = new ArrayList<>(List.of(profesional()));
        List<WeeklySchedule> horarios = new ArrayList<>(List.of(horario()));

        CatalogSnapshot snapshot = snapshotValido()
                .categories(categorias)
                .professionals(profesionales)
                .weeklySchedules(horarios)
                .build();

        categorias.add(categoria());
        profesionales.clear();
        horarios.add(horario());

        assertThat(snapshot.getCategories()).hasSize(1);
        assertThat(snapshot.getProfessionals()).hasSize(1);
        assertThat(snapshot.getWeeklySchedules()).hasSize(1);
    }

    @Test
    void las_listas_del_snapshot_no_se_pueden_modificar() {
        CatalogSnapshot snapshot = snapshotValido().build();

        assertThatThrownBy(() -> snapshot.getCategories().add(categoria()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getProfessionals().add(profesional()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getWeeklySchedules().add(horario()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
