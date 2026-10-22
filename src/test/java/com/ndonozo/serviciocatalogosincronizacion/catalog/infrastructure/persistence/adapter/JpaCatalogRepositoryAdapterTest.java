package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.WeeklySchedule;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.CatalogVersionMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalCategoryMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.ProfessionalMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaCatalogVersionRepository;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaProfessionalRepository;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCatalogRepositoryAdapterTest {

    private static final Instant CREADO = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-09-10T13:40:00Z");

    @Mock private JpaProfessionalRepository professionalRepository;
    @Mock private JpaProfessionalCategoryRepository categoryRepository;
    @Mock private JpaWeeklyScheduleRepository weeklySchedulesRepository;
    @Mock private JpaCatalogVersionRepository catalogVersionRepository;

    @Captor private ArgumentCaptor<List<ProfessionalCategoryEntity>> categoriesCaptor;
    @Captor private ArgumentCaptor<List<ProfessionalEntity>> professionalsCaptor;
    @Captor private ArgumentCaptor<List<WeeklyScheduleEntity>> schedulesCaptor;
    @Captor private ArgumentCaptor<CatalogVersionEntity> versionCaptor;

    private JpaCatalogRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCatalogRepositoryAdapter(
                professionalRepository, categoryRepository,
                weeklySchedulesRepository, catalogVersionRepository,
                new ProfessionalCategoryMapper(), new ProfessionalMapper(),
                new WeeklyScheduleMapper(), new CatalogVersionMapper());
    }

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

    @Test
    void saveProfessionalCategories_convierte_y_guarda_con_el_repositorio() {
        adapter.saveProfessionalCategories(List.of(categoria()));

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        assertThat(categoriesCaptor.getValue()).hasSize(1);
        assertThat(categoriesCaptor.getValue().get(0).getId()).isEqualTo(1L);
        assertThat(categoriesCaptor.getValue().get(0).getName()).isEqualTo("Clinica medica");
    }

    @Test
    void saveProfessionals_convierte_y_guarda_con_el_repositorio() {
        adapter.saveProfessionals(List.of(profesional()));

        verify(professionalRepository).saveAll(professionalsCaptor.capture());
        assertThat(professionalsCaptor.getValue()).hasSize(1);
        assertThat(professionalsCaptor.getValue().get(0).getId()).isEqualTo(15L);
        assertThat(professionalsCaptor.getValue().get(0).getCategoryId()).isEqualTo(1L);
    }

    @Test
    void saveWeeklySchedule_convierte_y_guarda_con_el_repositorio() {
        adapter.saveWeeklySchedule(List.of(horario()));

        verify(weeklySchedulesRepository).saveAll(schedulesCaptor.capture());
        assertThat(schedulesCaptor.getValue()).hasSize(1);
        assertThat(schedulesCaptor.getValue().get(0).getId()).isEqualTo(21L);
        assertThat(schedulesCaptor.getValue().get(0).getProfessionalId()).isEqualTo(15L);
    }

    @Test
    void una_lista_vacia_no_rompe_y_se_guarda_vacia() {
        adapter.saveProfessionals(List.of());

        verify(professionalRepository).saveAll(professionalsCaptor.capture());
        assertThat(professionalsCaptor.getValue()).isEmpty();
    }

    @Test
    void getCatalogVersion_devuelve_la_version_guardada() {
        CatalogVersionEntity entity = new CatalogVersionEntity();
        entity.setId(CatalogVersionEntity.SINGLE_ROW_ID);
        entity.setSnapshotVersion(7L);
        when(catalogVersionRepository.findById(CatalogVersionEntity.SINGLE_ROW_ID))
                .thenReturn(Optional.of(entity));

        CatalogVersion version = adapter.getCatalogVersion();

        assertThat(version.getSnapshotVersion()).isEqualTo(7L);
    }

    @Test
    void getCatalogVersion_falla_si_no_existe_la_fila() {
        when(catalogVersionRepository.findById(CatalogVersionEntity.SINGLE_ROW_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.getCatalogVersion())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("catalog_version has no row");
    }

    @Test
    void saveCatalogVersion_guarda_la_version_en_la_fila_unica() {
        adapter.saveCatalogVersion(CatalogVersion.builder().snapshotVersion(9).build());

        verify(catalogVersionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getId()).isEqualTo(CatalogVersionEntity.SINGLE_ROW_ID);
        assertThat(versionCaptor.getValue().getSnapshotVersion()).isEqualTo(9L);
    }
}
