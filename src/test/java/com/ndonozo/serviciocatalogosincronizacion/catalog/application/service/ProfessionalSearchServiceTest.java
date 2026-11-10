package com.ndonozo.serviciocatalogosincronizacion.catalog.application.service;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.Professional;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalSearchCriteria;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.in.SearchProfessionalsUseCase;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionalSearchServiceTest {

    @Mock
    private SearchProfessionalsUseCase searchProfessionals;

    @InjectMocks
    private ProfessionalSearchService service;

    @Test
    void la_busqueda_se_delega_al_caso_de_uso_y_devuelve_su_resultado() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().name("torres").build();
        Professional ana = Professional.builder().id(15L).categoryId(101L).firstName("Ana").lastName("Torres")
                .enabled(true).createdAt(Instant.parse("2026-09-01T03:00:00Z"))
                .updatedAt(Instant.parse("2026-11-06T14:19:29Z")).build();
        List<Professional> encontrados = List.of(ana);
        when(searchProfessionals.searchProfessionals(criteria)).thenReturn(encontrados);

        List<Professional> resultado = service.search(criteria);

        assertThat(resultado).isSameAs(encontrados);
        verify(searchProfessionals).searchProfessionals(criteria);
    }

    @Test
    void si_el_caso_de_uso_falla_la_excepcion_se_propaga() {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().build();
        IllegalStateException error = new IllegalStateException("base caida");
        when(searchProfessionals.searchProfessionals(criteria)).thenThrow(error);

        assertThatThrownBy(() -> service.search(criteria)).isSameAs(error);
    }
}
