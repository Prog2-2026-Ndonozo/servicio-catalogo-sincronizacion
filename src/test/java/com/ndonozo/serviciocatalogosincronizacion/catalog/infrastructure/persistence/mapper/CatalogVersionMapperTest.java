package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.mapper;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogVersion;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogVersionMapperTest {

    private final CatalogVersionMapper mapper = new CatalogVersionMapper();

    @Test
    void toEntity_siempre_usa_el_id_de_la_fila_unica() {
        CatalogVersion catalogVersion = CatalogVersion.builder()
                        .snapshotVersion(7).build();

        CatalogVersionEntity entity = mapper.toEntity(catalogVersion);



        assertThat(entity.getId()).isEqualTo((short) 1);
        assertThat(entity.getSnapshotVersion()).isEqualTo(7);
        // CatalogVersion con versión 7 -> la entidad debe tener id 1 y snapshotVersion 7
    }

    @Test
    void toDomain_toma_la_version_de_la_entidad() {
        CatalogVersionEntity entity = new CatalogVersionEntity();
        entity.setId((short) 1);
        entity.setSnapshotVersion(7L);

        CatalogVersion version = mapper.toDomain(entity);

        assertThat(version.getSnapshotVersion()).isEqualTo(7L);
    }

    @Test
    void null_devuelve_null_en_ambos_sentidos() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
