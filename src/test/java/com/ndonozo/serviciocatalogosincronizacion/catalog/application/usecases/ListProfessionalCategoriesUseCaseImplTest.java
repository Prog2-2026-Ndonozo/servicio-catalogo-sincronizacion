package com.ndonozo.serviciocatalogosincronizacion.catalog.application.usecases;

import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.model.ProfessionalCategory;
import com.ndonozo.serviciocatalogosincronizacion.catalog.domain.ports.out.ProfessionalCategoryReadPort;
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
class ListProfessionalCategoriesUseCaseImplTest {

    @Mock
    private ProfessionalCategoryReadPort readPort;

    @InjectMocks
    private ListProfessionalCategoriesUseCaseImpl useCase;

    private static ProfessionalCategory categoria(long id, String nombre) {
        return ProfessionalCategory.builder().id(id).name(nombre).description("").enabled(true)
                .createdAt(Instant.parse("2026-07-01T12:00:00Z")).updatedAt(Instant.parse("2026-09-10T13:40:00Z")).build();
    }

    @Test
    void delega_en_el_puerto_y_devuelve_su_resultado() {
        List<ProfessionalCategory> categorias = List.of(categoria(101, "Clinica medica"), categoria(102, "Pediatria"));
        when(readPort.findAll()).thenReturn(categorias);

        assertThat(useCase.listProfessionalCategories()).isSameAs(categorias);
        verify(readPort).findAll();
        verifyNoMoreInteractions(readPort);
    }

    @Test
    void sin_categorias_devuelve_una_lista_vacia() {
        when(readPort.findAll()).thenReturn(List.of());

        assertThat(useCase.listProfessionalCategories()).isEmpty();
    }

    @Test
    void si_el_puerto_falla_la_excepcion_se_propaga() {
        IllegalStateException error = new IllegalStateException("base caida");
        when(readPort.findAll()).thenThrow(error);

        assertThatThrownBy(() -> useCase.listProfessionalCategories()).isSameAs(error);
    }
}
