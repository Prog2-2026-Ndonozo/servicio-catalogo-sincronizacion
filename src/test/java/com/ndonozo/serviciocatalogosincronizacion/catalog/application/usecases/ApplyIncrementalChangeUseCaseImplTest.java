package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.FullSyncRequiredException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogChanges;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogUpdate;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.VersionWindow;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatedraCatalogReadPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProcessedEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyIncrementalChangeUseCaseImplTest {

    private static final Instant CREADO = Instant.parse("2026-09-01T03:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-11-06T14:19:29Z");
    private static final String EVENT_ID = "catalog-event-2-4f3b";

    @Mock
    private CatedraCatalogReadPort catedra;

    @Mock
    private CatalogRepositoryPort repository;

    @Mock
    private ProcessedEventPort processedEvents;

    private ApplyIncrementalChangeUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyIncrementalChangeUseCaseImpl(catedra, repository, processedEvents);
    }

    private static CatalogVersion version(long value) {
        return CatalogVersion.builder().snapshotVersion(value).build();
    }

    private static CatalogUpdate evento(String eventId, long newVersion) {
        return CatalogUpdate.builder().eventId(eventId).newVersion(newVersion).build();
    }

    private static VersionWindow ventana(long oldest, long current) {
        return VersionWindow.builder().oldestAvailableVersion(oldest).currentVersion(current).build();
    }

    private static Optional<CatalogChanges> cambios(long version, List<Long> categorias, List<Long> profesionales,
                                          List<Long> horarios) {
        return Optional.of(CatalogChanges.builder().version(version)
                .categoryIds(categorias).professionalIds(profesionales).weeklySchedulesIds(horarios).build());
    }

    private static Professional profesional(long id) {
        return Professional.builder().id(id).categoryId(101L).firstName("Ana").lastName("Torres")
                .enabled(false).createdAt(CREADO).updatedAt(ACTUALIZADO).build();
    }

    private static ProfessionalCategory categoria(long id) {
        return ProfessionalCategory.builder().id(id).name("Clinica medica").description("")
                .enabled(true).createdAt(CREADO).updatedAt(ACTUALIZADO).build();
    }

    private static WeeklySchedule horario(long id, long professionalId) {
        return WeeklySchedule.builder().id(id).professionalId(professionalId).dayOfWeek(MONDAY)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30).enabled(true).createdAt(CREADO).updatedAt(ACTUALIZADO).build();
    }

    // ---------- eventos que no hay que aplicar ----------

    @Test
    void un_evento_con_version_menor_a_la_local_se_ignora_sin_consultar_nada_mas() {
        when(repository.getCatalogVersion()).thenReturn(version(5));

        CatalogVersion resultado = useCase.applyIncrementalChange(evento(EVENT_ID, 3));

        assertThat(resultado.getSnapshotVersion()).isEqualTo(5);
        verifyNoInteractions(catedra, processedEvents);
        verify(repository).getCatalogVersion();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void un_evento_con_la_misma_version_que_la_local_se_ignora() {
        when(repository.getCatalogVersion()).thenReturn(version(5));

        CatalogVersion resultado = useCase.applyIncrementalChange(evento(EVENT_ID, 5));

        assertThat(resultado.getSnapshotVersion()).isEqualTo(5);
        verifyNoInteractions(catedra, processedEvents);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void un_evento_ya_procesado_se_ignora_sin_leer_de_redis_ni_guardar() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(true);

        CatalogVersion resultado = useCase.applyIncrementalChange(evento(EVENT_ID, 2));

        assertThat(resultado.getSnapshotVersion()).isEqualTo(1);
        verifyNoInteractions(catedra);
        verifyNoMoreInteractions(repository);
        verify(processedEvents, never()).markProcessed(any(), org.mockito.ArgumentMatchers.anyLong());
    }

    // ---------- camino normal ----------

    @Test
    void aplica_el_cambio_de_la_version_siguiente_y_registra_version_y_evento() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        when(catedra.fetchChanges(2L)).thenReturn(cambios(2, List.of(), List.of(201L), List.of()));
        List<Professional> profesionales = List.of(profesional(201));
        when(catedra.fetchProfessional(Set.of(201L))).thenReturn(profesionales);

        CatalogVersion resultado = useCase.applyIncrementalChange(evento(EVENT_ID, 2));

        assertThat(resultado.getSnapshotVersion()).isEqualTo(2);
        verify(repository).saveProfessionals(profesionales);

        ArgumentCaptor<CatalogVersion> version = ArgumentCaptor.forClass(CatalogVersion.class);
        verify(repository).saveCatalogVersion(version.capture());
        assertThat(version.getValue().getSnapshotVersion()).isEqualTo(2);
        verify(processedEvents).markProcessed(EVENT_ID, 2);
    }

    @Test
    void las_colecciones_sin_cambios_se_guardan_vacias_sin_romper() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        when(catedra.fetchChanges(2L)).thenReturn(cambios(2, List.of(), List.of(201L), List.of()));
        when(catedra.fetchProfessional(Set.of(201L))).thenReturn(List.of(profesional(201)));

        useCase.applyIncrementalChange(evento(EVENT_ID, 2));

        verify(catedra).fetchCategories(Set.of());
        verify(catedra).fetchWeeklySchedule(Set.of());
        verify(repository).saveProfessionalCategories(List.of());
        verify(repository).saveWeeklySchedule(List.of());
    }

    @Test
    void guarda_las_tres_colecciones_cuando_una_version_las_modifica() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        when(catedra.fetchChanges(2L)).thenReturn(cambios(2, List.of(101L), List.of(201L), List.of(301L)));
        List<ProfessionalCategory> categorias = List.of(categoria(101));
        List<Professional> profesionales = List.of(profesional(201));
        List<WeeklySchedule> horarios = List.of(horario(301, 201));
        when(catedra.fetchCategories(Set.of(101L))).thenReturn(categorias);
        when(catedra.fetchProfessional(Set.of(201L))).thenReturn(profesionales);
        when(catedra.fetchWeeklySchedule(Set.of(301L))).thenReturn(horarios);

        useCase.applyIncrementalChange(evento(EVENT_ID, 2));

        verify(repository).saveProfessionalCategories(categorias);
        verify(repository).saveProfessionals(profesionales);
        verify(repository).saveWeeklySchedule(horarios);
    }

    @Test
    void la_version_y_el_evento_se_registran_despues_de_guardar_los_datos() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        when(catedra.fetchChanges(2L)).thenReturn(cambios(2, List.of(101L), List.of(201L), List.of(301L)));
        when(catedra.fetchCategories(Set.of(101L))).thenReturn(List.of(categoria(101)));
        when(catedra.fetchProfessional(Set.of(201L))).thenReturn(List.of(profesional(201)));
        when(catedra.fetchWeeklySchedule(Set.of(301L))).thenReturn(List.of(horario(301, 201)));

        useCase.applyIncrementalChange(evento(EVENT_ID, 2));

        // el orden entre las tres colecciones no importa (FK diferidas); si importa que todas vayan antes de la version
        InOrder categorias = inOrder(repository, processedEvents);
        categorias.verify(repository).saveProfessionalCategories(any());
        categorias.verify(repository).saveCatalogVersion(any(CatalogVersion.class));
        categorias.verify(processedEvents).markProcessed(EVENT_ID, 2);

        InOrder profesionales = inOrder(repository);
        profesionales.verify(repository).saveProfessionals(any());
        profesionales.verify(repository).saveCatalogVersion(any(CatalogVersion.class));

        InOrder horarios = inOrder(repository);
        horarios.verify(repository).saveWeeklySchedule(any());
        horarios.verify(repository).saveCatalogVersion(any(CatalogVersion.class));
    }

    // ---------- evento que salta varias versiones ----------

    @Test
    void si_el_evento_salta_versiones_lee_los_cambios_de_todas_y_junta_los_ids_sin_repetir() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        when(catedra.fetchChanges(2L)).thenReturn(cambios(2, List.of(), List.of(201L), List.of()));
        when(catedra.fetchChanges(3L)).thenReturn(cambios(3, List.of(101L), List.of(201L, 202L), List.of()));
        when(catedra.fetchChanges(4L)).thenReturn(cambios(4, List.of(), List.of(201L), List.of(301L)));
        when(catedra.fetchCategories(Set.of(101L))).thenReturn(List.of(categoria(101)));
        when(catedra.fetchProfessional(Set.of(201L, 202L))).thenReturn(List.of(profesional(201), profesional(202)));
        when(catedra.fetchWeeklySchedule(Set.of(301L))).thenReturn(List.of(horario(301, 201)));

        CatalogVersion resultado = useCase.applyIncrementalChange(evento(EVENT_ID, 4));

        assertThat(resultado.getSnapshotVersion()).isEqualTo(4);
        verify(catedra, never()).fetchChanges(1L);
        verify(catedra, never()).fetchChanges(5L);
        // cada coleccion se lee una sola vez, con los ids de las tres versiones juntos
        verify(catedra).fetchProfessional(Set.of(201L, 202L));
        verify(processedEvents).markProcessed(EVENT_ID, 4);
    }

    // ---------- fallos: no debe quedar nada a medias ----------

    @Test
    void si_redis_no_responde_al_leer_los_cambios_no_se_guarda_nada_y_la_excepcion_se_propaga() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        CatedraUnavailableException error = new CatedraUnavailableException("Redis de la catedra no disponible", new RuntimeException());
        when(catedra.fetchChanges(2L)).thenThrow(error);

        assertThatThrownBy(() -> useCase.applyIncrementalChange(evento(EVENT_ID, 2))).isSameAs(error);

        verify(repository, never()).saveProfessionals(any());
        verify(repository, never()).saveCatalogVersion(any());
        verify(processedEvents, never()).markProcessed(any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void si_falta_una_version_intermedia_se_exige_el_snapshot_sin_guardar_version_ni_evento() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        when(catedra.fetchChanges(2L)).thenReturn(cambios(2, List.of(), List.of(201L), List.of()));
        when(catedra.fetchChanges(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.applyIncrementalChange(evento(EVENT_ID, 3)))
                .isInstanceOf(FullSyncRequiredException.class)
                .hasMessageContaining("3");

        verify(repository, never()).saveProfessionals(any());
        verify(repository, never()).saveCatalogVersion(any());
        verify(processedEvents, never()).markProcessed(any(), anyLong());
    }

    @Test
    void unos_cambios_ilegibles_siguen_siendo_un_dato_invalido_y_no_piden_snapshot() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        InvalidCatalogDataException error = new InvalidCatalogDataException("changes:2 ilegible");
        when(catedra.fetchChanges(2L)).thenThrow(error);

        assertThatThrownBy(() -> useCase.applyIncrementalChange(evento(EVENT_ID, 2))).isSameAs(error);

        verify(repository, never()).saveCatalogVersion(any());
        verify(processedEvents, never()).markProcessed(any(), anyLong());
    }

    @Test
    void si_falla_al_guardar_no_se_avanza_la_version_ni_se_registra_el_evento() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(1, 60));
        when(catedra.fetchChanges(2L)).thenReturn(cambios(2, List.of(), List.of(201L), List.of()));
        List<Professional> profesionales = List.of(profesional(201));
        when(catedra.fetchProfessional(Set.of(201L))).thenReturn(profesionales);
        IllegalStateException error = new IllegalStateException("base caida");
        doThrow(error).when(repository).saveProfessionals(profesionales);

        assertThatThrownBy(() -> useCase.applyIncrementalChange(evento(EVENT_ID, 2))).isSameAs(error);

        verify(repository, never()).saveCatalogVersion(any());
        verify(processedEvents, never()).markProcessed(any(), org.mockito.ArgumentMatchers.anyLong());
    }

    // ---------- version local fuera de la ventana (ADR-0029) ----------

    @Test
    void si_la_version_local_es_menor_que_la_mas_antigua_disponible_exige_el_snapshot_completo() {
        when(repository.getCatalogVersion()).thenReturn(version(3));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(4, 7));

        assertThatThrownBy(() -> useCase.applyIncrementalChange(evento(EVENT_ID, 7)))
                .isInstanceOf(FullSyncRequiredException.class)
                .hasMessageContaining("3")
                .hasMessageContaining("4");

        verify(catedra, never()).fetchChanges(anyLong());
        verify(repository, never()).saveProfessionals(any());
        verify(repository, never()).saveCatalogVersion(any());
        verify(processedEvents, never()).markProcessed(any(), anyLong());
    }

    @Test
    void con_la_version_local_igual_a_la_mas_antigua_disponible_sigue_siendo_incremental() {
        when(repository.getCatalogVersion()).thenReturn(version(4));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(4, 5));
        when(catedra.fetchChanges(5L)).thenReturn(cambios(5, List.of(), List.of(201L), List.of()));
        when(catedra.fetchProfessional(Set.of(201L))).thenReturn(List.of(profesional(201)));

        CatalogVersion resultado = useCase.applyIncrementalChange(evento(EVENT_ID, 5));

        assertThat(resultado.getSnapshotVersion()).isEqualTo(5);
        verify(processedEvents).markProcessed(EVENT_ID, 5);
    }

    @Test
    void con_la_base_sin_sincronizar_exige_el_snapshot_completo_aunque_la_ventana_empiece_en_cero() {
        when(repository.getCatalogVersion()).thenReturn(version(0));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        when(catedra.fetchVersionWindow()).thenReturn(ventana(0, 7));

        assertThatThrownBy(() -> useCase.applyIncrementalChange(evento(EVENT_ID, 7)))
                .isInstanceOf(FullSyncRequiredException.class);

        verify(catedra, never()).fetchChanges(anyLong());
        verify(repository, never()).saveCatalogVersion(any());
    }

    @Test
    void si_no_se_puede_leer_la_ventana_no_se_guarda_nada_y_la_excepcion_se_propaga() {
        when(repository.getCatalogVersion()).thenReturn(version(1));
        when(processedEvents.isProcessed(EVENT_ID)).thenReturn(false);
        CatedraUnavailableException error = new CatedraUnavailableException("Redis de la catedra no disponible", new RuntimeException());
        when(catedra.fetchVersionWindow()).thenThrow(error);

        assertThatThrownBy(() -> useCase.applyIncrementalChange(evento(EVENT_ID, 2))).isSameAs(error);

        verify(catedra, never()).fetchChanges(anyLong());
        verify(repository, never()).saveCatalogVersion(any());
    }

    // ---------- transaccion ----------

    @Test
    void el_caso_de_uso_corre_dentro_de_una_transaccion() throws NoSuchMethodException {
        Transactional transaccional = ApplyIncrementalChangeUseCaseImpl.class
                .getMethod("applyIncrementalChange", CatalogUpdate.class)
                .getAnnotation(Transactional.class);

        assertThat(transaccional).isNotNull();
    }
}
