package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.exception.InvalidCatalogDataException;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalSearchPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchProfessionalsUseCaseImplTest {

    private static final Instant CREADO = Instant.parse("2026-09-01T03:00:00Z");
    private static final Instant ACTUALIZADO = Instant.parse("2026-11-06T14:19:29Z");

    @Mock
    private ProfessionalSearchPort searchPort;

    @InjectMocks
    private SearchProfessionalsUseCaseImpl useCase;

    private static Professional profesional(long id, String nombre, String apellido) {
        return Professional.builder().id(id).categoryId(101L).firstName(nombre).lastName(apellido)
                .enabled(true).createdAt(CREADO).updatedAt(ACTUALIZADO).build();
    }

    @Test
    void delega_la_busqueda_en_el_puerto_con_los_mismos_criterios_y_devuelve_su_resultado() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder()
                .categoryId(101L).name("ana").enabled(true).build();
        List<Professional> encontrados = List.of(profesional(15, "Ana", "Torres"), profesional(201, "Ana", "Gomez"));
        when(searchPort.search(criteria)).thenReturn(encontrados);

        List<Professional> resultado = useCase.searchProfessionals(criteria);

        assertThat(resultado).isSameAs(encontrados);
        verify(searchPort).search(criteria);
        verifyNoMoreInteractions(searchPort);
    }

    @Test
    void sin_filtros_igual_consulta_al_puerto() {
        ProfessionalSearchCriteria sinFiltros = ProfessionalSearchCriteria.builder().build();
        when(searchPort.search(sinFiltros)).thenReturn(List.of(profesional(15, "Ana", "Torres")));

        assertThat(useCase.searchProfessionals(sinFiltros)).hasSize(1);
    }

    @Test
    void una_busqueda_sin_resultados_devuelve_una_lista_vacia() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().name("zzz").build();
        when(searchPort.search(criteria)).thenReturn(List.of());

        assertThat(useCase.searchProfessionals(criteria)).isEmpty();
    }

    @Test
    void si_el_puerto_falla_la_excepcion_se_propaga_sin_ser_capturada() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().build();
        IllegalStateException error = new IllegalStateException("base caida");
        when(searchPort.search(criteria)).thenThrow(error);

        assertThatThrownBy(() -> useCase.searchProfessionals(criteria)).isSameAs(error);
    }

    @Test
    void un_dato_invalido_del_puerto_tambien_se_propaga() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().build();
        InvalidCatalogDataException error = new InvalidCatalogDataException("fila invalida");
        when(searchPort.search(criteria)).thenThrow(error);

        assertThatThrownBy(() -> useCase.searchProfessionals(criteria)).isSameAs(error);
    }
}
