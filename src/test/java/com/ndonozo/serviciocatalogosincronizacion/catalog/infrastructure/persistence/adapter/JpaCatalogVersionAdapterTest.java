package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper.CatalogVersionMapper;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.repository.JpaCatalogVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCatalogVersionAdapterTest {

    @Mock
    private JpaCatalogVersionRepository repository;

    @Captor
    private ArgumentCaptor<CatalogVersionEntity> versionCaptor;

    private JpaCatalogVersionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCatalogVersionAdapter(repository, new CatalogVersionMapper());
    }

    @Test
    void getCatalogVersion_devuelve_la_version_guardada() {
        CatalogVersionEntity entity = new CatalogVersionEntity();
        entity.setId(CatalogVersionEntity.SINGLE_ROW_ID);
        entity.setSnapshotVersion(7L);
        when(repository.findById(CatalogVersionEntity.SINGLE_ROW_ID)).thenReturn(Optional.of(entity));

        CatalogVersion version = adapter.getCatalogVersion();

        assertThat(version.getSnapshotVersion()).isEqualTo(7L);
    }

    @Test
    void getCatalogVersion_falla_si_no_existe_la_fila() {
        when(repository.findById(CatalogVersionEntity.SINGLE_ROW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.getCatalogVersion())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("catalog_version has no row");
    }

    @Test
    void saveCatalogVersion_guarda_la_version_en_la_fila_unica() {
        adapter.saveCatalogVersion(CatalogVersion.builder().snapshotVersion(9).build());

        verify(repository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getId()).isEqualTo(CatalogVersionEntity.SINGLE_ROW_ID);
        assertThat(versionCaptor.getValue().getSnapshotVersion()).isEqualTo(9L);
    }
}
