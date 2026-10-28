package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogSnapshot;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatalogRepositoryPort;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.CatedraSnapshotPort;
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

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyFullSnapshotUseCaseImplTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    @Mock
    private CatedraSnapshotPort snapshotPort;

    @Mock
    private CatalogRepositoryPort repository;

    private ApplyFullSnapshotUseCaseImpl useCase;

    private final List<ProfessionalCategory> categorias = List.of(ProfessionalCategory.builder()
            .id(1L).name("Clinica medica").description("").enabled(true)
            .createdAt(CREADO).updatedAt(ACTUALIZADO).build());

    private final List<Professional> profesionales = List.of(Professional.builder()
            .id(15L).categoryId(1L).firstName("Ana").lastName("Gomez").enabled(true)
            .createdAt(CREADO).updatedAt(ACTUALIZADO).build());

    private final List<WeeklySchedule> horarios = List.of(WeeklySchedule.builder()
            .id(21L).professionalId(15L).dayOfWeek(MONDAY)
            .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0))
            .slotDurationMinutes(30).enabled(true)
            .createdAt(CREADO).updatedAt(ACTUALIZADO).build());

    private CatalogSnapshot snapshot;

    @BeforeEach
    void preparaCasoDeUso() {
        useCase = new ApplyFullSnapshotUseCaseImpl(snapshotPort, repository);
        snapshot = CatalogSnapshot.builder()
                .snapshotVersion(7L)
                .categories(categorias)
                .professionals(profesionales)
                .weeklySchedules(horarios)
                .build();
    }

    @Test
    // camino feliz
    void guarda_las_tres_colecciones_y_despues_la_version_del_snapshot() {
        when(snapshotPort.fetchSnapshot()).thenReturn(snapshot);

        CatalogVersion aplicada = useCase.applyFullSnapshot();

        InOrder orden = inOrder(repository);
        orden.verify(repository).saveProfessionalCategories(categorias);
        orden.verify(repository).saveProfessionals(profesionales);
        orden.verify(repository).saveWeeklySchedule(horarios);
        ArgumentCaptor<CatalogVersion> guardada = ArgumentCaptor.forClass(CatalogVersion.class);
        orden.verify(repository).saveCatalogVersion(guardada.capture());

        assertThat(guardada.getValue().getSnapshotVersion()).isEqualTo(7L);
        assertThat(aplicada).isSameAs(guardada.getValue());
    }

    @Test
    // redundante porque los snapshot no venian vacios supuestamente
    void un_snapshot_vacio_tambien_avanza_la_version() {
        CatalogSnapshot vacio = CatalogSnapshot.builder()
                .snapshotVersion(3L)
                .categories(List.of())
                .professionals(List.of())
                .weeklySchedules(List.of())
                .build();
        when(snapshotPort.fetchSnapshot()).thenReturn(vacio);

        CatalogVersion aplicada = useCase.applyFullSnapshot();

        assertThat(aplicada.getSnapshotVersion()).isEqualTo(3L);
        verify(repository).saveCatalogVersion(aplicada);
    }

    @Test
    // Test importante que prueba que se haga de manera Transactional
    void si_falla_al_guardar_categorias_no_se_guarda_nada_mas() {
        when(snapshotPort.fetchSnapshot()).thenReturn(snapshot);
        doThrow(new IllegalStateException("fallo de base")).when(repository).saveProfessionalCategories(any());

        assertThatThrownBy(() -> useCase.applyFullSnapshot()).isInstanceOf(IllegalStateException.class);

        verify(repository, never()).saveProfessionals(any());
        verify(repository, never()).saveWeeklySchedule(any());
        verify(repository, never()).saveCatalogVersion(any());
    }

    @Test
    void si_falla_a_mitad_de_camino_al_guardar_profesionales_la_version_local_no_avanza() {
        when(snapshotPort.fetchSnapshot()).thenReturn(snapshot);
        doThrow(new IllegalStateException("fallo de base")).when(repository).saveProfessionals(any());

        assertThatThrownBy(() -> useCase.applyFullSnapshot()).isInstanceOf(IllegalStateException.class);

        verify(repository, never()).saveWeeklySchedule(any());
        verify(repository, never()).saveCatalogVersion(any());
    }

    @Test
    void si_falla_al_guardar_horarios_la_version_local_no_avanza() {
        when(snapshotPort.fetchSnapshot()).thenReturn(snapshot);
        doThrow(new IllegalStateException("fallo de base")).when(repository).saveWeeklySchedule(any());

        assertThatThrownBy(() -> useCase.applyFullSnapshot()).isInstanceOf(IllegalStateException.class);

        verify(repository, never()).saveCatalogVersion(any());
    }

    @Test
    void si_la_catedra_no_responde_no_se_toca_el_repositorio_y_el_error_se_propaga() {
        CatedraUnavailableException fallo = new CatedraUnavailableException("Catedra answered HTTP 503", 503);
        when(snapshotPort.fetchSnapshot()).thenThrow(fallo);

        assertThatThrownBy(() -> useCase.applyFullSnapshot()).isSameAs(fallo);

        verifyNoInteractions(repository);
    }

    @Test
    void si_la_catedra_manda_un_dato_invalido_no_se_toca_el_repositorio_y_el_error_se_propaga() {
        InvalidCatalogDataException fallo = new InvalidCatalogDataException("Professional firstName must not be blank");
        when(snapshotPort.fetchSnapshot()).thenThrow(fallo);

        assertThatThrownBy(() -> useCase.applyFullSnapshot()).isSameAs(fallo);

        verifyNoInteractions(repository);
    }

    @Test
    // Revisar este Test , no se bien que hace
    void el_metodo_es_transaccional_para_que_un_fallo_deshaga_todo() throws NoSuchMethodException {
        assertThat(ApplyFullSnapshotUseCaseImpl.class.getMethod("applyFullSnapshot")
                .isAnnotationPresent(Transactional.class)).isTrue();
    }
}

// Esto garantiza basicamente el orden, que un error en cualquier paso  impide que se guarde la version
// No nos garantiza que la base de datos realmente Deshaga lo que ya se guardo ya que estos son Mocks
// Para probar eso vamos a necesitar un PostgreSQL de verdad: evaluar opciones como TestContainers (preguntar)