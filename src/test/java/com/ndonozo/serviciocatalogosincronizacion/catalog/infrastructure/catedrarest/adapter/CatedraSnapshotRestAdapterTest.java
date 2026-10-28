package com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.adapter;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.CatedraUnavailableException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.CatalogSnapshot;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.client.CatedraRestClient;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.dto.CatedraSnapshotResponse;
import com.ndonozo.serviciocatalogosincronizacion.catalog.infrastructure.catedrarest.mapper.SnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatedraSnapshotRestAdapterTest {

    @Mock
    private CatedraRestClient client;           // simulamos el cliente

    @Mock
    private SnapshotMapper mapper;              //simulamos el mapper

    private CatedraSnapshotRestAdapter adapter;

    private final CatedraSnapshotResponse response =
            new CatedraSnapshotResponse(7L, List.of(), List.of(), List.of());

    @BeforeEach
    void preparaAdaptador() {
        adapter = new CatedraSnapshotRestAdapter(client, mapper);
    }


    // El adaptador le pasa al mapper exactamente el DTO que dio el cliente y devuelve exactamente el objeto que dio el mapper
    // isSameAs, la misma instancia



    @Test
    void traduce_lo_que_devuelve_el_cliente_y_entrega_el_resultado_del_mapper() {
        CatalogSnapshot snapshot = CatalogSnapshot.builder()
                .snapshotVersion(7L)
                .categories(List.of())
                .professionals(List.of())
                .weeklySchedules(List.of())
                .build();
        when(client.fetchSnapshot()).thenReturn(response);
        when(mapper.toDomain(response)).thenReturn(snapshot);

        CatalogSnapshot resultado = adapter.fetchSnapshot();

        assertThat(resultado).isSameAs(snapshot);
    }



    // CatedraUnavailableException llega intacta y el mapper nunca se llama (verifyNo interactions)

    @Test
    void si_el_cliente_falla_propaga_el_error_y_no_traduce_nada() {
        CatedraUnavailableException fallo = new CatedraUnavailableException("Catedra answered HTTP 503", 503);
        when(client.fetchSnapshot()).thenThrow(fallo);

        assertThatThrownBy(() -> adapter.fetchSnapshot()).isSameAs(fallo);
        verifyNoInteractions(mapper);
    }


    // Invalid catalog data exception llega iuntacta
    @Test
    void si_el_mapper_rechaza_un_dato_propaga_el_error_del_dominio() {
        InvalidCatalogDataException fallo = new InvalidCatalogDataException("Professional firstName must not be blank");
        when(client.fetchSnapshot()).thenReturn(response);
        when(mapper.toDomain(response)).thenThrow(fallo);

        assertThatThrownBy(() -> adapter.fetchSnapshot()).isSameAs(fallo);
    }
}
