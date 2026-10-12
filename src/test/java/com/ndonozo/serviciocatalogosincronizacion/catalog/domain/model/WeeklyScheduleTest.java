package com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WeeklyScheduleTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    private static WeeklySchedule.WeeklyScheduleBuilder scheduleValido() {
        return WeeklySchedule.builder()
                .id(21L)
                .professionalId(15L)
                .dayOfWeek(MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .enabled(true)
                .createdAt(CREADO)
                .updatedAt(ACTUALIZADO);
    }

    @Test
    void validacion_con_datos_recibidos() {
        WeeklySchedule schedule = scheduleValido().build();

        assertThat(schedule.getId()).isEqualTo(21L);
        assertThat(schedule.getProfessionalId()).isEqualTo(15L);
        assertThat(schedule.getDayOfWeek()).isEqualTo(MONDAY);
        assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(schedule.isEnabled()).isTrue();
        assertThat(schedule.getCreatedAt()).isEqualTo(CREADO);
        assertThat(schedule.getUpdatedAt()).isEqualTo(ACTUALIZADO);
    }

    @ParameterizedTest
    @CsvSource({
            "09:00, 09:00", // Iguales; que no acepte turnos que empiezan y terminan a la misma hora
            "10:00, 09:00", // Inicio posterior a fin
            "22:00, 02:00"  // validamos que no cruce la medianoche
    })
    void validacion_de_tiempo_incorrecto_se_rechaza(LocalTime inicio, LocalTime fin){
        assertThatThrownBy(() -> scheduleValido()
                .startTime(inicio)
                .endTime(fin)
                .build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("startTime");



    }
    @ParameterizedTest

    @CsvSource({
            "08:00, 09:00", //
            "09:00, 12:00", // Probamos con horarios validos
            "00:00, 01:00"  //
    })

    void validacion_de_tiempo_correcto_se_acepta(LocalTime inicio, LocalTime fin){
        WeeklySchedule schedule = scheduleValido()
                .startTime(inicio)
                .endTime(fin)
                .build();
        assertThat(schedule.getStartTime()).isEqualTo(inicio);
        assertThat(schedule.getEndTime()).isEqualTo(fin);
    }
    @ParameterizedTest
    @ValueSource (ints = {0,-1,-30})
    void rechaza_slots_menor_o_igual_a_cero(int slotInvalido){
        assertThatThrownBy( ()->scheduleValido()
                .slotDurationMinutes(slotInvalido)
                .build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("slotDurationMinutes");


    }

    @ParameterizedTest
    @ValueSource (ints = {7,40,50,120})
    void rechaza_rango_que_no_es_multiplo(int slotQueNoDivide){
        assertThatThrownBy(()-> scheduleValido()
                .startTime(LocalTime.of(9,0))
                .endTime(LocalTime.of(12,0))
                .slotDurationMinutes(slotQueNoDivide)
                .build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("slotDurationMinutes");


    }
    @ParameterizedTest
    @ValueSource (ints = {30,45,60,90,180})

    void acepta_slots_validos_rango (int slotQueDivide){
        WeeklySchedule schedule = scheduleValido().slotDurationMinutes(slotQueDivide).build();

        assertThat(schedule.getSlotDurationMinutes()).isEqualTo(slotQueDivide);
    }

    @Test
    void rechaza_rangos_menores_a_un_minuto(){
        assertThatThrownBy(()-> scheduleValido()
                .startTime(LocalTime.of(9,0,0))
                .endTime(LocalTime.of(9,0,30))
                .build())
                .isInstanceOf(InvalidCatalogDataException.class)
                .hasMessageContaining("slotDurationMinutes");

    }


}
