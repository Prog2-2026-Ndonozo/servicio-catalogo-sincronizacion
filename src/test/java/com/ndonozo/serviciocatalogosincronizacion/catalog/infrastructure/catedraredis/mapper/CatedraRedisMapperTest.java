package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogChanges;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.VersionWindow;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.CatalogChangesRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.CategoryRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.ProfessionalRedisDto;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedraredis.dto.WeeklyScheduleRedisDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatedraRedisMapperTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    private final CatedraRedisMapper mapper = new CatedraRedisMapper();

    private static ProfessionalRedisDto profesional(Long id, Long categoryId, String firstName, String lastName) {
        return new ProfessionalRedisDto(id, categoryId, firstName, lastName, true, CREADO, ACTUALIZADO);
    }

    private static Map<String, String> metadataValida() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("currentVersion", "7");
        metadata.put("oldestAvailableVersion", "4");
        metadata.put("publishedAt", "2026-09-10T13:44:59Z");
        return metadata;
    }

    // ----- Professional -----

    @Test
    void traduce_un_profesional_conservando_todos_los_campos() {
        Professional profesional = mapper.toDomain(profesional(15L, 1L, "Ana", "Gomez"));

        assertThat(profesional.getId()).isEqualTo(15L);
        assertThat(profesional.getCategoryId()).isEqualTo(1L);
        assertThat(profesional.getFirstName()).isEqualTo("Ana");
        assertThat(profesional.getLastName()).isEqualTo("Gomez");
        assertThat(profesional.isEnabled()).isTrue();
        assertThat(profesional.getCreatedAt()).isEqualTo(CREADO);
        assertThat(profesional.getUpdatedAt()).isEqualTo(ACTUALIZADO);
    }

    @Test
    void conserva_un_profesional_deshabilitado_porque_es_una_baja_logica() {
        ProfessionalRedisDto dto = new ProfessionalRedisDto(15L, 1L, "Ana", "Gomez", false, CREADO, ACTUALIZADO);

        assertThat(mapper.toDomain(dto).isEnabled()).isFalse();
    }

    @Test
    void rechaza_un_profesional_sin_id() {
        assertThatThrownBy(() -> mapper.toDomain(profesional(null, 1L, "Ana", "Gomez")))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_un_profesional_sin_categoria() {
        assertThatThrownBy(() -> mapper.toDomain(profesional(15L, null, "Ana", "Gomez")))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t"})
    void rechaza_un_profesional_con_nombre_en_blanco(String blanco) {
        assertThatThrownBy(() -> mapper.toDomain(profesional(15L, 1L, blanco, "Gomez")))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_un_profesional_con_nombre_o_apellido_nulo() {
        assertThatThrownBy(() -> mapper.toDomain(profesional(15L, 1L, null, "Gomez")))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(profesional(15L, 1L, "Ana", null)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_un_profesional_sin_fechas() {
        assertThatThrownBy(() -> mapper.toDomain(
                new ProfessionalRedisDto(15L, 1L, "Ana", "Gomez", true, null, ACTUALIZADO)))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(
                new ProfessionalRedisDto(15L, 1L, "Ana", "Gomez", true, CREADO, null)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    // ----- ProfessionalCategory -----

    private static CategoryRedisDto categoria(Long id, String name, String description) {
        return new CategoryRedisDto(id, name, description, true, CREADO, ACTUALIZADO);
    }

    @Test
    void traduce_una_categoria_conservando_todos_los_campos() {
        ProfessionalCategory categoria = mapper.toDomain(categoria(1L, "Clinica medica", "Atencion clinica general"));

        assertThat(categoria.getId()).isEqualTo(1L);
        assertThat(categoria.getName()).isEqualTo("Clinica medica");
        assertThat(categoria.getDescription()).isEqualTo("Atencion clinica general");
        assertThat(categoria.isEnabled()).isTrue();
        assertThat(categoria.getCreatedAt()).isEqualTo(CREADO);
        assertThat(categoria.getUpdatedAt()).isEqualTo(ACTUALIZADO);
    }

    @Test
    void una_descripcion_nula_se_traduce_a_texto_vacio() {
        assertThat(mapper.toDomain(categoria(1L, "Clinica medica", null)).getDescription()).isEmpty();
    }

    @Test
    void conserva_una_categoria_deshabilitada_porque_es_una_baja_logica() {
        CategoryRedisDto dto = new CategoryRedisDto(1L, "Clinica medica", "x", false, CREADO, ACTUALIZADO);

        assertThat(mapper.toDomain(dto).isEnabled()).isFalse();
    }

    @Test
    void rechaza_una_categoria_sin_id() {
        assertThatThrownBy(() -> mapper.toDomain(categoria(null, "Clinica medica", "x")))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t"})
    void rechaza_una_categoria_con_nombre_en_blanco(String blanco) {
        assertThatThrownBy(() -> mapper.toDomain(categoria(1L, blanco, "x")))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_una_categoria_con_nombre_nulo() {
        assertThatThrownBy(() -> mapper.toDomain(categoria(1L, null, "x")))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_una_categoria_sin_fechas() {
        assertThatThrownBy(() -> mapper.toDomain(new CategoryRedisDto(1L, "A", "x", true, null, ACTUALIZADO)))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(new CategoryRedisDto(1L, "A", "x", true, CREADO, null)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    // ----- WeeklySchedule -----

    private static WeeklyScheduleRedisDto horario(Long id, Long professionalId, DayOfWeek dia,
                                                  LocalTime inicio, LocalTime fin, int minutos) {
        return new WeeklyScheduleRedisDto(id, professionalId, dia, inicio, fin, minutos, true, CREADO, ACTUALIZADO);
    }

    private static WeeklyScheduleRedisDto horarioValido() {
        return horario(21L, 15L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30);
    }

    @Test
    void traduce_un_horario_conservando_todos_los_campos() {
        WeeklySchedule horario = mapper.toDomain(horarioValido());

        assertThat(horario.getId()).isEqualTo(21L);
        assertThat(horario.getProfessionalId()).isEqualTo(15L);
        assertThat(horario.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(horario.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(horario.getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(horario.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(horario.isEnabled()).isTrue();
        assertThat(horario.getCreatedAt()).isEqualTo(CREADO);
        assertThat(horario.getUpdatedAt()).isEqualTo(ACTUALIZADO);
    }

    @Test
    void conserva_un_horario_deshabilitado_porque_es_una_baja_logica() {
        WeeklyScheduleRedisDto dto = new WeeklyScheduleRedisDto(21L, 15L, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), 30, false, CREADO, ACTUALIZADO);

        assertThat(mapper.toDomain(dto).isEnabled()).isFalse();
    }

    @Test
    void rechaza_un_horario_sin_id_profesional_dia_o_horas() {
        LocalTime inicio = LocalTime.of(9, 0);
        LocalTime fin = LocalTime.of(12, 0);

        assertThatThrownBy(() -> mapper.toDomain(horario(null, 15L, DayOfWeek.MONDAY, inicio, fin, 30)))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(horario(21L, null, DayOfWeek.MONDAY, inicio, fin, 30)))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(horario(21L, 15L, null, inicio, fin, 30)))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(horario(21L, 15L, DayOfWeek.MONDAY, null, fin, 30)))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(horario(21L, 15L, DayOfWeek.MONDAY, inicio, null, 30)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_un_horario_cuyo_inicio_no_es_anterior_al_fin() {
        assertThatThrownBy(() -> mapper.toDomain(
                horario(21L, 15L, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(12, 0), 30)))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(() -> mapper.toDomain(
                horario(21L, 15L, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(9, 0), 30)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -30})
    void rechaza_un_horario_con_duracion_de_turno_no_positiva(int minutos) {
        assertThatThrownBy(() -> mapper.toDomain(
                horario(21L, 15L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), minutos)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_un_horario_cuya_duracion_de_turno_no_divide_el_rango() {
        assertThatThrownBy(() -> mapper.toDomain(
                horario(21L, 15L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 25)))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    // ----- CatalogChanges -----

    @Test
    void traduce_los_cambios_de_una_version() {
        CatalogChangesRedisDto dto = new CatalogChangesRedisDto(7L,
                new CatalogChangesRedisDto.Changes(List.of(1L), List.of(15L, 16L), List.of()));

        CatalogChanges cambios = mapper.toDomain(dto);

        assertThat(cambios.getVersion()).isEqualTo(7L);
        assertThat(cambios.getCategoryIds()).containsExactly(1L);
        assertThat(cambios.getProfessionalIds()).containsExactly(15L, 16L);
        assertThat(cambios.getWeeklySchedulesIds()).isEmpty();
    }

    @Test
    void acepta_una_version_con_las_tres_listas_vacias() {
        CatalogChangesRedisDto dto = new CatalogChangesRedisDto(7L,
                new CatalogChangesRedisDto.Changes(List.of(), List.of(), List.of()));

        CatalogChanges cambios = mapper.toDomain(dto);

        assertThat(cambios.getCategoryIds()).isEmpty();
        assertThat(cambios.getProfessionalIds()).isEmpty();
        assertThat(cambios.getWeeklySchedulesIds()).isEmpty();
    }

    @Test
    void rechaza_cambios_sin_el_objeto_changes() {
        assertThatThrownBy(() -> mapper.toDomain(new CatalogChangesRedisDto(7L, null)))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("changes")
                .hasMessageContaining("7");
    }

    @Test
    void rechaza_cambios_con_una_lista_nula() {
        assertThatThrownBy(() -> mapper.toDomain(new CatalogChangesRedisDto(7L,
                new CatalogChangesRedisDto.Changes(List.of(), null, List.of()))))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @Test
    void rechaza_cambios_con_version_no_positiva() {
        assertThatThrownBy(() -> mapper.toDomain(new CatalogChangesRedisDto(0L,
                new CatalogChangesRedisDto.Changes(List.of(), List.of(), List.of()))))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    // ----- VersionWindow -----

    @Test
    void traduce_la_metadata_del_contrato_a_una_ventana_de_versiones() {
        VersionWindow ventana = mapper.toVersionWindow(metadataValida());

        assertThat(ventana.getCurrentVersion()).isEqualTo(7L);
        assertThat(ventana.getOldestAvailableVersion()).isEqualTo(4L);
    }

    @Test
    void ignora_los_campos_de_metadata_que_no_usa() {
        Map<String, String> metadata = metadataValida();
        metadata.put("campoNuevo", "x");

        assertThat(mapper.toVersionWindow(metadata).getCurrentVersion()).isEqualTo(7L);
    }

    @Test
    void tolera_espacios_alrededor_de_los_numeros() {
        Map<String, String> metadata = metadataValida();
        metadata.put("currentVersion", " 7 ");

        assertThat(mapper.toVersionWindow(metadata).getCurrentVersion()).isEqualTo(7L);
    }

    @Test
    void rechaza_metadata_sin_current_version() {
        Map<String, String> metadata = metadataValida();
        metadata.remove("currentVersion");

        assertThatThrownBy(() -> mapper.toVersionWindow(metadata))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("currentVersion");
    }

    @Test
    void rechaza_metadata_sin_oldest_available_version() {
        Map<String, String> metadata = metadataValida();
        metadata.remove("oldestAvailableVersion");

        assertThatThrownBy(() -> mapper.toVersionWindow(metadata))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("oldestAvailableVersion");
    }

    @Test
    void rechaza_metadata_vacia() {
        assertThatThrownBy(() -> mapper.toVersionWindow(Map.of()))
                .isInstanceOf(InvalidCatalogDataException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"siete", "", "7.5", "1e3"})
    void rechaza_una_version_que_no_es_un_entero(String noEntero) {
        Map<String, String> metadata = metadataValida();
        metadata.put("currentVersion", noEntero);

        assertThatThrownBy(() -> mapper.toVersionWindow(metadata))
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("currentVersion");
    }

    @Test
    void rechaza_una_ventana_incoherente_con_oldest_mayor_que_current() {
        Map<String, String> metadata = metadataValida();
        metadata.put("oldestAvailableVersion", "9");

        assertThatThrownBy(() -> mapper.toVersionWindow(metadata))
                .isInstanceOf(InvalidCatalogDataException.class);
    }
}
